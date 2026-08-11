#include <jni.h>
#include <dirent.h>
#include <fcntl.h>
#include <linux/input.h>
#include <poll.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <unistd.h>

#include <cerrno>
#include <algorithm>
#include <chrono>
#include <cstring>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>

namespace {

constexpr const char* kThorTouchNode = "/dev/rstouch";
constexpr const char* kThorUpperTouchscreenName = "fts_ts";
constexpr const char* kThorTouchUnsupported = "THOR_TOUCH_UNSUPPORTED";
constexpr int kThorUpperTouchSlotCount = 22;
constexpr int kThorCoexistTouchSlot = 10;
constexpr int kThorCoexistTrackingId = 65535;

std::mutex thor_touch_lock;
int thor_touch_fd = -1;
bool thor_touch_active = false;
int thor_touch_raw_width = 0;
int thor_touch_raw_height = 0;
int thor_touch_last_x = 0;
int thor_touch_last_y = 0;

void set_event(input_event& event, int type, int code, int value) {
    gettimeofday(&event.time, nullptr);
    event.type = static_cast<__u16>(type);
    event.code = static_cast<__u16>(code);
    event.value = value;
}

bool write_event_frame(int fd, const std::vector<input_event>& events, std::string& error) {
    const char* data = reinterpret_cast<const char*>(events.data());
    size_t remaining = events.size() * sizeof(input_event);
    while (remaining > 0) {
        ssize_t written = write(fd, data, remaining);
        if (written < 0) {
            if (errno == EINTR) {
                continue;
            }
            error = std::strerror(errno);
            return false;
        }
        if (written == 0) {
            error = "short write";
            return false;
        }
        data += written;
        remaining -= static_cast<size_t>(written);
    }
    return true;
}

bool find_thor_upper_touchscreen(int logical_width, int logical_height,
        int& raw_width, int& raw_height) {
    DIR* directory = opendir("/dev/input");
    if (directory == nullptr) {
        return false;
    }
    bool found = false;
    dirent* entry;
    while ((entry = readdir(directory)) != nullptr && !found) {
        if (std::strncmp(entry->d_name, "event", 5) != 0) {
            continue;
        }
        std::string path = std::string("/dev/input/") + entry->d_name;
        int fd = open(path.c_str(), O_RDONLY | O_NONBLOCK | O_CLOEXEC);
        if (fd < 0) {
            continue;
        }
        char name[128]{};
        input_absinfo slot{};
        input_absinfo position_x{};
        input_absinfo position_y{};
        bool matches = ioctl(fd, EVIOCGNAME(sizeof(name)), name) >= 0
                && std::strcmp(name, kThorUpperTouchscreenName) == 0
                && ioctl(fd, EVIOCGABS(ABS_MT_SLOT), &slot) == 0
                && ioctl(fd, EVIOCGABS(ABS_MT_POSITION_X), &position_x) == 0
                && ioctl(fd, EVIOCGABS(ABS_MT_POSITION_Y), &position_y) == 0
                && slot.minimum == 0
                && slot.maximum + 1 == kThorUpperTouchSlotCount;
        if (matches) {
            int candidate_width = position_x.maximum - position_x.minimum;
            int candidate_height = position_y.maximum - position_y.minimum;
            bool logical_match = (candidate_width == logical_width
                    && candidate_height == logical_height)
                    || (candidate_width == logical_height
                    && candidate_height == logical_width);
            if (logical_match) {
                raw_width = candidate_width;
                raw_height = candidate_height;
                found = true;
            }
        }
        close(fd);
    }
    closedir(directory);
    return found;
}

void logical_to_raw_touch(int logical_x, int logical_y, int rotation,
        int raw_width, int raw_height, int& raw_x, int& raw_y) {
    switch (rotation) {
        case 1:  // Surface.ROTATION_90
            raw_x = raw_width - logical_y;
            raw_y = logical_x;
            break;
        case 2:  // Surface.ROTATION_180
            raw_x = raw_width - logical_x;
            raw_y = raw_height - logical_y;
            break;
        case 3:  // Surface.ROTATION_270
            raw_x = logical_y;
            raw_y = raw_height - logical_x;
            break;
        default:
            raw_x = logical_x;
            raw_y = logical_y;
            break;
    }
    raw_x = std::max(1, std::min(raw_width - 1, raw_x));
    raw_y = std::max(1, std::min(raw_height - 1, raw_y));
}

bool emit_thor_touch_up_locked(std::string& error) {
    if (!thor_touch_active) {
        return true;
    }
    if (thor_touch_fd < 0) {
        thor_touch_active = false;
        error = "touch device closed";
        return false;
    }
    std::vector<input_event> events(4);
    set_event(events[0], EV_ABS, ABS_MT_SLOT, kThorCoexistTouchSlot);
    set_event(events[1], EV_ABS, ABS_MT_TRACKING_ID, -1);
    set_event(events[2], EV_KEY, BTN_TOUCH, 0);
    set_event(events[3], EV_SYN, SYN_REPORT, 0);
    bool ok = write_event_frame(thor_touch_fd, events, error);
    thor_touch_active = false;
    return ok;
}

void close_thor_touch_locked() {
    std::string ignored;
    emit_thor_touch_up_locked(ignored);
    if (thor_touch_fd >= 0) {
        close(thor_touch_fd);
        thor_touch_fd = -1;
    }
    thor_touch_raw_width = 0;
    thor_touch_raw_height = 0;
}

std::string emit_thor_mapped_touch(int action, int logical_x, int logical_y,
        int logical_width, int logical_height, int rotation) {
    std::lock_guard<std::mutex> guard(thor_touch_lock);
    if (action == 0) {  // MotionEvent.ACTION_DOWN
        close_thor_touch_locked();
        int raw_width = 0;
        int raw_height = 0;
        if (logical_width <= 0 || logical_height <= 0
                || !find_thor_upper_touchscreen(logical_width, logical_height,
                        raw_width, raw_height)) {
            return kThorTouchUnsupported;
        }
        int fd = open(kThorTouchNode, O_RDWR | O_CLOEXEC);
        if (fd < 0) {
            return kThorTouchUnsupported;
        }
        thor_touch_fd = fd;
        thor_touch_raw_width = raw_width;
        thor_touch_raw_height = raw_height;
        logical_to_raw_touch(logical_x, logical_y, rotation, raw_width, raw_height,
                thor_touch_last_x, thor_touch_last_y);
        std::vector<input_event> events(9);
        set_event(events[0], EV_ABS, ABS_MT_SLOT, kThorCoexistTouchSlot);
        set_event(events[1], EV_ABS, ABS_MT_TRACKING_ID, kThorCoexistTrackingId);
        set_event(events[2], EV_KEY, BTN_TOUCH, 1);
        set_event(events[3], EV_ABS, ABS_MT_POSITION_X, thor_touch_last_x);
        set_event(events[4], EV_ABS, ABS_MT_POSITION_Y, thor_touch_last_y);
        set_event(events[5], EV_ABS, ABS_MT_TOUCH_MAJOR, 1);
        set_event(events[6], EV_ABS, ABS_MT_WIDTH_MAJOR, 1);
        set_event(events[7], EV_ABS, ABS_MT_PRESSURE, 1);
        set_event(events[8], EV_SYN, SYN_REPORT, 0);
        std::string error;
        if (!write_event_frame(thor_touch_fd, events, error)) {
            close_thor_touch_locked();
            return "Thor mapped touch down failed: " + error;
        }
        thor_touch_active = true;
        return "ok";
    }

    if (!thor_touch_active || thor_touch_fd < 0) {
        return "Thor mapped touch is not active";
    }
    if (action == 1 || action == 3) {  // ACTION_UP / ACTION_CANCEL
        std::string error;
        bool ok = emit_thor_touch_up_locked(error);
        if (thor_touch_fd >= 0) {
            close(thor_touch_fd);
            thor_touch_fd = -1;
        }
        return ok ? "ok" : "Thor mapped touch release failed: " + error;
    }
    if (action != 2) {  // MotionEvent.ACTION_MOVE
        return "Thor mapped touch action unsupported";
    }

    logical_to_raw_touch(logical_x, logical_y, rotation, thor_touch_raw_width,
            thor_touch_raw_height, thor_touch_last_x, thor_touch_last_y);
    std::vector<input_event> events(4);
    set_event(events[0], EV_ABS, ABS_MT_SLOT, kThorCoexistTouchSlot);
    set_event(events[1], EV_ABS, ABS_MT_POSITION_X, thor_touch_last_x);
    set_event(events[2], EV_ABS, ABS_MT_POSITION_Y, thor_touch_last_y);
    set_event(events[3], EV_SYN, SYN_REPORT, 0);
    std::string error;
    if (!write_event_frame(thor_touch_fd, events, error)) {
        close_thor_touch_locked();
        return "Thor mapped touch move failed: " + error;
    }
    return "ok";
}

bool emit_event(int fd, int type, int code, int value, std::ostringstream& out, const char* label) {
    input_event event{};
    event.type = static_cast<__u16>(type);
    event.code = static_cast<__u16>(code);
    event.value = value;
    ssize_t written = write(fd, &event, sizeof(event));
    if (written != sizeof(event)) {
        out << label << " failed: " << std::strerror(errno);
        return false;
    }
    return true;
}

std::string trim(const std::string& value) {
    size_t left = value.find_first_not_of(" \t\r\n");
    if (left == std::string::npos) {
        return "";
    }
    size_t right = value.find_last_not_of(" \t\r\n");
    return value.substr(left, right - left + 1);
}

int gamepad_key_code_from_name(std::string name) {
    name = trim(name);
    for (char& ch : name) {
        if (ch >= 'a' && ch <= 'z') {
            ch = static_cast<char>(ch - 'a' + 'A');
        }
    }
    if (name == "A" || name == "BTN_SOUTH") return BTN_SOUTH;
    if (name == "B" || name == "BTN_EAST") return BTN_EAST;
    if (name == "X" || name == "BTN_NORTH") return BTN_NORTH;
    if (name == "Y" || name == "BTN_WEST") return BTN_WEST;
    if (name == "LB" || name == "L1" || name == "BTN_TL") return BTN_TL;
    if (name == "RB" || name == "R1" || name == "BTN_TR") return BTN_TR;
    if (name == "LT" || name == "L2" || name == "BTN_TL2") return BTN_TL2;
    if (name == "RT" || name == "R2" || name == "BTN_TR2") return BTN_TR2;
    if (name == "SELECT" || name == "BACK" || name == "BTN_SELECT") return BTN_SELECT;
    if (name == "START" || name == "MENU" || name == "BTN_START") return BTN_START;
    if (name == "L3" || name == "BTN_THUMBL") return BTN_THUMBL;
    if (name == "R3" || name == "BTN_THUMBR") return BTN_THUMBR;
    if (name == "HOME" || name == "MODE" || name == "BTN_MODE") return BTN_MODE;
    if (name == "DPAD_UP" || name == "UP") return BTN_DPAD_UP;
    if (name == "DPAD_DOWN" || name == "DOWN") return BTN_DPAD_DOWN;
    if (name == "DPAD_LEFT" || name == "LEFT") return BTN_DPAD_LEFT;
    if (name == "DPAD_RIGHT" || name == "RIGHT") return BTN_DPAD_RIGHT;
    if (name.rfind("KEY_", 0) == 0) {
        try {
            return std::stoi(name.substr(4));
        } catch (...) {
            return -1;
        }
    }
    return -1;
}

std::vector<int> parse_gamepad_combo(const char* combo) {
    std::vector<int> result;
    if (combo == nullptr) {
        return result;
    }
    std::string raw(combo);
    size_t start = 0;
    while (start < raw.size()) {
        size_t end = raw.find('+', start);
        std::string token = raw.substr(start, end == std::string::npos ? std::string::npos : end - start);
        int code = gamepad_key_code_from_name(token);
        if (code >= 0) {
            result.push_back(code);
        }
        if (end == std::string::npos) {
            break;
        }
        start = end + 1;
    }
    return result;
}

struct SequenceItem {
    int type = 0;
    int code = 0;
    int value = 0;
    int delay_ms = 0;
};

bool parse_sequence_item(const std::string& raw, SequenceItem& item) {
    std::vector<int> values;
    size_t start = 0;
    while (start <= raw.size()) {
        size_t end = raw.find(',', start);
        std::string token = raw.substr(start, end == std::string::npos ? std::string::npos : end - start);
        try {
            values.push_back(std::stoi(trim(token)));
        } catch (...) {
            return false;
        }
        if (end == std::string::npos) {
            break;
        }
        start = end + 1;
    }
    if (values.size() != 4) {
        return false;
    }
    item.type = values[0];
    item.code = values[1];
    item.value = values[2];
    item.delay_ms = values[3];
    if (item.delay_ms < 0) item.delay_ms = 0;
    if (item.delay_ms > 500) item.delay_ms = 500;
    return true;
}

void track_sequence_key(std::vector<int>& pressed_keys, int code, int value) {
    auto existing = std::find(pressed_keys.begin(), pressed_keys.end(), code);
    if (value == 0) {
        if (existing != pressed_keys.end()) {
            pressed_keys.erase(existing);
        }
    } else if (existing == pressed_keys.end()) {
        pressed_keys.push_back(code);
    }
}

void release_sequence_keys(int fd, const std::vector<int>& pressed_keys) {
    if (pressed_keys.empty()) {
        return;
    }
    std::ostringstream ignored;
    for (auto key = pressed_keys.rbegin(); key != pressed_keys.rend(); ++key) {
        emit_event(fd, EV_KEY, *key, 0, ignored, "sequence recovery release");
    }
    emit_event(fd, EV_SYN, SYN_REPORT, 0, ignored, "sequence recovery sync");
}

std::string emit_evdev_sequence(const char* path, const char* sequence) {
    std::ostringstream out;
    if (path == nullptr || std::strlen(path) == 0) {
        return "empty input path";
    }
    if (sequence == nullptr || std::strlen(sequence) == 0) {
        return "empty sequence";
    }

    int fd = open(path, O_WRONLY | O_NONBLOCK);
    if (fd < 0) {
        out << "open " << path << " failed: " << std::strerror(errno);
        return out.str();
    }

    std::string raw(sequence);
    if (raw.rfind("seq:", 0) == 0) {
        raw = raw.substr(4);
    }

    int count = 0;
    std::vector<int> pressed_keys;
    size_t start = 0;
    while (start < raw.size()) {
        size_t end = raw.find(';', start);
        std::string token = raw.substr(start, end == std::string::npos ? std::string::npos : end - start);
        SequenceItem item;
        if (parse_sequence_item(token, item)) {
            if (item.delay_ms > 0) {
                usleep(static_cast<useconds_t>(item.delay_ms) * 1000);
            }
            if (!emit_event(fd, item.type, item.code, item.value, out, "sequence event")) {
                release_sequence_keys(fd, pressed_keys);
                close(fd);
                return out.str();
            }
            if (item.type == EV_KEY) {
                track_sequence_key(pressed_keys, item.code, item.value);
            }
            if (item.type != EV_SYN) {
                if (!emit_event(fd, EV_SYN, SYN_REPORT, 0, out, "sequence sync")) {
                    release_sequence_keys(fd, pressed_keys);
                    close(fd);
                    return out.str();
                }
            }
            count++;
        }
        if (end == std::string::npos) {
            break;
        }
        start = end + 1;
    }

    close(fd);
    if (count == 0) {
        return "no sequence events";
    }
    out << "evdev sequence ok events=" << count;
    return out.str();
}

std::string emit_evdev_combo(const char* path, const char* combo, int hold_ms) {
    if (combo != nullptr && std::strncmp(combo, "seq:", 4) == 0) {
        return emit_evdev_sequence(path, combo);
    }

    std::ostringstream out;
    if (path == nullptr || std::strlen(path) == 0) {
        return "empty input path";
    }
    std::vector<int> keys = parse_gamepad_combo(combo);
    if (keys.empty()) {
        return "no keys parsed";
    }
    if (hold_ms < 20) hold_ms = 20;
    if (hold_ms > 1000) hold_ms = 1000;

    int fd = open(path, O_WRONLY | O_NONBLOCK);
    if (fd < 0) {
        out << "open " << path << " failed: " << std::strerror(errno);
        return out.str();
    }

    for (int key : keys) {
        if (!emit_event(fd, EV_KEY, key, 1, out, "key down")) {
            close(fd);
            return out.str();
        }
    }
    if (!emit_event(fd, EV_SYN, SYN_REPORT, 0, out, "sync down")) {
        close(fd);
        return out.str();
    }
    usleep(static_cast<useconds_t>(hold_ms) * 1000);
    for (auto it = keys.rbegin(); it != keys.rend(); ++it) {
        if (!emit_event(fd, EV_KEY, *it, 0, out, "key up")) {
            close(fd);
            return out.str();
        }
    }
    if (!emit_event(fd, EV_SYN, SYN_REPORT, 0, out, "sync up")) {
        close(fd);
        return out.str();
    }
    close(fd);

    out << "evdev combo ok keys=" << keys.size();
    return out.str();
}

long long event_time_ms(const input_event& event) {
    return static_cast<long long>(event.time.tv_sec) * 1000LL
            + static_cast<long long>(event.time.tv_usec) / 1000LL;
}

std::string capture_evdev_sequence(const char* path, int duration_ms) {
    constexpr size_t kMaxCapturedEvents = 1024;
    std::ostringstream out;
    if (path == nullptr || std::strlen(path) == 0) {
        return "empty input path";
    }
    int fd = open(path, O_RDONLY | O_NONBLOCK);
    if (fd < 0) {
        out << "open " << path << " failed: " << std::strerror(errno);
        return out.str();
    }
    if (duration_ms < 500) duration_ms = 500;
    if (duration_ms > 10000) duration_ms = 10000;

    std::vector<std::string> items;
    long long last_time_ms = 0;
    const auto deadline = std::chrono::steady_clock::now()
            + std::chrono::milliseconds(duration_ms);
    pollfd pfd{};
    pfd.fd = fd;
    pfd.events = POLLIN;
    while (items.size() < kMaxCapturedEvents) {
        const auto now = std::chrono::steady_clock::now();
        if (now >= deadline) {
            break;
        }
        long long remaining_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
                deadline - now).count();
        int wait_ms = remaining_ms > 40 ? 40 : static_cast<int>(remaining_ms);
        if (wait_ms < 1) {
            wait_ms = 1;
        }
        int poll_result = poll(&pfd, 1, wait_ms);
        if (poll_result < 0) {
            out << "poll failed: " << std::strerror(errno);
            close(fd);
            return out.str();
        }
        if (poll_result == 0 || (pfd.revents & POLLIN) == 0) {
            continue;
        }

        input_event event{};
        while (items.size() < kMaxCapturedEvents) {
            if (std::chrono::steady_clock::now() >= deadline) {
                break;
            }
            ssize_t read_bytes = read(fd, &event, sizeof(event));
            if (read_bytes < 0) {
                if (errno != EAGAIN && errno != EWOULDBLOCK) {
                    out << "read failed: " << std::strerror(errno);
                    close(fd);
                    return out.str();
                }
                break;
            }
            if (read_bytes != sizeof(event)) {
                out << "short read";
                close(fd);
                return out.str();
            }
            if (event.type == EV_SYN) {
                continue;
            }

            long long now_ms = event_time_ms(event);
            int delay_ms = 0;
            if (last_time_ms > 0 && now_ms > last_time_ms) {
                long long delta = now_ms - last_time_ms;
                delay_ms = static_cast<int>(delta > 500 ? 500 : delta);
            }
            last_time_ms = now_ms;

            std::ostringstream item;
            item << event.type << "," << event.code << "," << event.value << "," << delay_ms;
            items.push_back(item.str());
        }
    }
    close(fd);

    if (items.empty()) {
        return "NO_EVENTS";
    }
    out << "seq:";
    for (size_t i = 0; i < items.size(); i++) {
        if (i > 0) {
            out << ";";
        }
        out << items[i];
    }
    return out.str();
}

int axis_value_from_normalized(int fd, int axis, float value) {
    input_absinfo info{};
    if (ioctl(fd, EVIOCGABS(axis), &info) == 0 && info.maximum > info.minimum) {
        const int center = info.minimum + (info.maximum - info.minimum) / 2;
        if (value >= 0.0f) {
            return center + static_cast<int>(value * static_cast<float>(info.maximum - center));
        }
        return center + static_cast<int>(value * static_cast<float>(center - info.minimum));
    }
    return static_cast<int>(value * 32767.0f);
}

std::string emit_native_right_stick(const char* path, float x, float y,
        int axis_code_x, int axis_code_y) {
    std::ostringstream out;
    if (path == nullptr || std::strlen(path) == 0) {
        return "empty input path";
    }
    if (x < -1.0f) x = -1.0f;
    if (x > 1.0f) x = 1.0f;
    if (y < -1.0f) y = -1.0f;
    if (y > 1.0f) y = 1.0f;

    int fd = open(path, O_WRONLY | O_NONBLOCK);
    if (fd < 0) {
        out << "open " << path << " failed: " << std::strerror(errno);
        return out.str();
    }

    int axis_x = axis_value_from_normalized(fd, axis_code_x, x);
    int axis_y = axis_value_from_normalized(fd, axis_code_y, y);
    if (!emit_event(fd, EV_ABS, axis_code_x, axis_x, out, "right stick x")) {
        close(fd);
        return out.str();
    }
    if (!emit_event(fd, EV_ABS, axis_code_y, axis_y, out, "right stick y")) {
        close(fd);
        return out.str();
    }
    if (!emit_event(fd, EV_SYN, SYN_REPORT, 0, out, "right stick sync")) {
        close(fd);
        return out.str();
    }
    close(fd);

    out << "native right stick ok";
    return out.str();
}

}  // namespace

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_emitEvdevCombo(JNIEnv* env, jclass,
        jstring path, jstring combo, jint hold_ms) {
    const char* raw_path = env->GetStringUTFChars(path, nullptr);
    const char* raw_combo = env->GetStringUTFChars(combo, nullptr);
    std::string result = emit_evdev_combo(raw_path, raw_combo, static_cast<int>(hold_ms));
    env->ReleaseStringUTFChars(combo, raw_combo);
    env->ReleaseStringUTFChars(path, raw_path);
    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_captureEvdevSequence(JNIEnv* env, jclass,
        jstring path, jint duration_ms) {
    const char* raw_path = env->GetStringUTFChars(path, nullptr);
    std::string result = capture_evdev_sequence(raw_path, static_cast<int>(duration_ms));
    env->ReleaseStringUTFChars(path, raw_path);
    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_emitNativeRightStick(JNIEnv* env, jclass,
        jstring path, jfloat x, jfloat y, jint axis_x, jint axis_y) {
    const char* raw_path = env->GetStringUTFChars(path, nullptr);
    std::string result = emit_native_right_stick(raw_path, static_cast<float>(x),
            static_cast<float>(y), static_cast<int>(axis_x), static_cast<int>(axis_y));
    env->ReleaseStringUTFChars(path, raw_path);
    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_emitThorMappedTouch(JNIEnv* env, jclass,
        jint action, jint x, jint y, jint width, jint height, jint rotation) {
    std::string result = emit_thor_mapped_touch(static_cast<int>(action),
            static_cast<int>(x), static_cast<int>(y), static_cast<int>(width),
            static_cast<int>(height), static_cast<int>(rotation));
    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_releaseThorMappedTouch(JNIEnv*, jclass) {
    std::lock_guard<std::mutex> guard(thor_touch_lock);
    close_thor_touch_locked();
}
