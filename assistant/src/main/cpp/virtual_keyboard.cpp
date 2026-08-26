#include <jni.h>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <unistd.h>

#include <cerrno>
#include <cstring>
#include <mutex>
#include <string>

namespace {

constexpr int kFirstKeyboardCode = 1;
// The Thor-verified KM-0 device advertised only EV_KEY 1..255. Keeping the product
// device below BTN_MISC prevents mouse or gamepad classification.
constexpr int kLastKeyboardCode = 255;

std::mutex keyboard_lock;
int keyboard_fd = -1;
bool pressed_keys[kLastKeyboardCode + 1]{};
int pressed_key_count = 0;

void reset_pressed_keys_locked() {
    std::memset(pressed_keys, 0, sizeof(pressed_keys));
    pressed_key_count = 0;
}

bool emit_event(int fd, int type, int code, int value, std::string& error) {
    input_event event{};
    gettimeofday(&event.time, nullptr);
    event.type = static_cast<__u16>(type);
    event.code = static_cast<__u16>(code);
    event.value = value;
    const char* data = reinterpret_cast<const char*>(&event);
    size_t remaining = sizeof(event);
    while (remaining > 0) {
        ssize_t written = write(fd, data, remaining);
        if (written < 0 && errno == EINTR) continue;
        if (written <= 0) {
            error = written == 0 ? "short write" : std::strerror(errno);
            return false;
        }
        data += written;
        remaining -= static_cast<size_t>(written);
    }
    return true;
}

bool release_keys_locked(std::string& error) {
    if (keyboard_fd < 0 || pressed_key_count == 0) {
        reset_pressed_keys_locked();
        return true;
    }
    bool ok = true;
    for (int key_code = kFirstKeyboardCode; key_code <= kLastKeyboardCode; key_code++) {
        if (pressed_keys[key_code]
                && !emit_event(keyboard_fd, EV_KEY, key_code, 0, error)) {
            ok = false;
        }
    }
    std::string sync_error;
    if (!emit_event(keyboard_fd, EV_SYN, SYN_REPORT, 0, sync_error)) {
        if (error.empty()) error = sync_error;
        ok = false;
    }
    reset_pressed_keys_locked();
    return ok;
}

void close_locked() {
    if (keyboard_fd < 0) {
        reset_pressed_keys_locked();
        return;
    }
    std::string ignored;
    release_keys_locked(ignored);
    ioctl(keyboard_fd, UI_DEV_DESTROY);
    close(keyboard_fd);
    keyboard_fd = -1;
    reset_pressed_keys_locked();
}

std::string open_keyboard_locked() {
    if (keyboard_fd >= 0) return "ok: virtual keyboard already open";
    int fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK | O_CLOEXEC);
    if (fd < 0) return std::string("open /dev/uinput failed: ") + std::strerror(errno);

    auto fail = [&](const char* operation) {
        std::string result = std::string(operation) + ": " + std::strerror(errno);
        close(fd);
        return result;
    };
    if (ioctl(fd, UI_SET_EVBIT, EV_KEY) < 0) return fail("UI_SET_EVBIT EV_KEY failed");
    for (int key_code = kFirstKeyboardCode; key_code <= kLastKeyboardCode; key_code++) {
        if (ioctl(fd, UI_SET_KEYBIT, key_code) < 0) {
            return fail("UI_SET_KEYBIT failed");
        }
    }

    uinput_user_dev device{};
    std::strncpy(device.name, "Heimdall Virtual Keyboard", UINPUT_MAX_NAME_SIZE - 1);
    device.id.bustype = BUS_VIRTUAL;
    device.id.vendor = 0x484d;
    device.id.product = 0x564b;
    device.id.version = 1;
    if (write(fd, &device, sizeof(device)) != sizeof(device)) {
        return fail("uinput device setup failed");
    }
    if (ioctl(fd, UI_DEV_CREATE) < 0) return fail("UI_DEV_CREATE failed");
    keyboard_fd = fd;
    reset_pressed_keys_locked();
    // Match the Thor-verified mouse route: allow Android InputReader to enumerate the
    // new device before the first queued key frame.
    usleep(100000);
    return "ok: Heimdall Virtual Keyboard open";
}

std::string open_keyboard() {
    std::lock_guard<std::mutex> guard(keyboard_lock);
    return open_keyboard_locked();
}

std::string emit_key(int key_code, int key_value) {
    std::lock_guard<std::mutex> guard(keyboard_lock);
    if (key_code < kFirstKeyboardCode || key_code > kLastKeyboardCode) {
        return "unsupported virtual keyboard key code";
    }
    if (key_value != 0 && key_value != 1) {
        return "unsupported virtual keyboard key value";
    }
    if (keyboard_fd < 0) {
        std::string opened = open_keyboard_locked();
        if (keyboard_fd < 0) return opened;
    }

    bool already_pressed = pressed_keys[key_code];
    if ((key_value != 0) == already_pressed) {
        return "ok: virtual keyboard key unchanged";
    }

    std::string error;
    if (!emit_event(keyboard_fd, EV_KEY, key_code, key_value, error)) {
        close_locked();
        return "virtual keyboard key failed: " + error;
    }
    if (key_value != 0) {
        pressed_keys[key_code] = true;
        pressed_key_count++;
    } else {
        pressed_keys[key_code] = false;
        pressed_key_count--;
    }
    if (!emit_event(keyboard_fd, EV_SYN, SYN_REPORT, 0, error)) {
        close_locked();
        return "virtual keyboard sync failed: " + error;
    }
    return "ok: virtual keyboard key";
}

std::string release_keys() {
    std::lock_guard<std::mutex> guard(keyboard_lock);
    std::string error;
    if (!release_keys_locked(error)) {
        close_locked();
        return "virtual keyboard all-up failed: " + error;
    }
    return "ok: virtual keyboard keys released";
}

std::string release_keyboard() {
    std::lock_guard<std::mutex> guard(keyboard_lock);
    close_locked();
    return "ok: virtual keyboard released";
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_openVirtualKeyboard(JNIEnv* env, jclass) {
    std::string result = open_keyboard();
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_emitVirtualKeyboardKey(
        JNIEnv* env, jclass, jint key_code, jint key_value) {
    std::string result = emit_key(key_code, key_value);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_releaseVirtualKeyboardKeys(
        JNIEnv* env, jclass) {
    std::string result = release_keys();
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_releaseVirtualKeyboard(JNIEnv* env, jclass) {
    std::string result = release_keyboard();
    return env->NewStringUTF(result.c_str());
}
