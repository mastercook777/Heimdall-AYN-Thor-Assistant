#include <jni.h>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <unistd.h>

#include <algorithm>
#include <cerrno>
#include <cstring>
#include <mutex>
#include <string>

namespace {

std::mutex mouse_lock;
int mouse_fd = -1;
int pressed_button = 0;

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

void release_button_locked() {
    if (mouse_fd < 0 || pressed_button == 0) return;
    std::string ignored;
    emit_event(mouse_fd, EV_KEY, pressed_button, 0, ignored);
    emit_event(mouse_fd, EV_SYN, SYN_REPORT, 0, ignored);
    pressed_button = 0;
}

void close_locked() {
    release_button_locked();
    if (mouse_fd < 0) return;
    ioctl(mouse_fd, UI_DEV_DESTROY);
    close(mouse_fd);
    mouse_fd = -1;
}

std::string open_mouse_locked() {
    if (mouse_fd >= 0) return "ok: virtual mouse already open";
    int fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK | O_CLOEXEC);
    if (fd < 0) return std::string("open /dev/uinput failed: ") + std::strerror(errno);

    auto fail = [&](const char* operation) {
        std::string result = std::string(operation) + ": " + std::strerror(errno);
        close(fd);
        return result;
    };
    if (ioctl(fd, UI_SET_EVBIT, EV_KEY) < 0) return fail("UI_SET_EVBIT EV_KEY failed");
    for (int button : {BTN_LEFT, BTN_RIGHT, BTN_MIDDLE}) {
        if (ioctl(fd, UI_SET_KEYBIT, button) < 0) return fail("UI_SET_KEYBIT failed");
    }
    if (ioctl(fd, UI_SET_EVBIT, EV_REL) < 0
            || ioctl(fd, UI_SET_RELBIT, REL_X) < 0
            || ioctl(fd, UI_SET_RELBIT, REL_Y) < 0
            || ioctl(fd, UI_SET_RELBIT, REL_WHEEL) < 0) {
        return fail("relative mouse setup failed");
    }

    uinput_user_dev device{};
    std::strncpy(device.name, "Heimdall Virtual Mouse", UINPUT_MAX_NAME_SIZE - 1);
    device.id.bustype = BUS_VIRTUAL;
    device.id.vendor = 0x484d;
    device.id.product = 0x564d;
    device.id.version = 1;
    if (write(fd, &device, sizeof(device)) != sizeof(device)) {
        return fail("uinput device setup failed");
    }
    if (ioctl(fd, UI_DEV_CREATE) < 0) return fail("UI_DEV_CREATE failed");
    mouse_fd = fd;
    // Give Android InputReader a bounded window to enumerate the new device before the
    // first queued frame arrives. Subsequent sessions reuse the open descriptor.
    usleep(100000);
    return "ok: Heimdall Virtual Mouse open";
}

std::string open_mouse() {
    std::lock_guard<std::mutex> guard(mouse_lock);
    return open_mouse_locked();
}

std::string emit_frame(int dx, int dy, int wheel, int button, int button_value) {
    std::lock_guard<std::mutex> guard(mouse_lock);
    if (mouse_fd < 0) {
        std::string opened = open_mouse_locked();
        if (mouse_fd < 0) return opened;
    }
    if (button != 0 && button != BTN_LEFT && button != BTN_RIGHT && button != BTN_MIDDLE) {
        return "unsupported virtual mouse button";
    }
    dx = std::max(-2048, std::min(dx, 2048));
    dy = std::max(-2048, std::min(dy, 2048));
    wheel = std::max(-32, std::min(wheel, 32));
    std::string error;
    if ((dx != 0 && !emit_event(mouse_fd, EV_REL, REL_X, dx, error))
            || (dy != 0 && !emit_event(mouse_fd, EV_REL, REL_Y, dy, error))
            || (wheel != 0 && !emit_event(mouse_fd, EV_REL, REL_WHEEL, wheel, error))) {
        close_locked();
        return "virtual mouse movement failed: " + error;
    }
    if (button != 0) {
        if (button_value != 0 && pressed_button != 0 && pressed_button != button) {
            release_button_locked();
        }
        if (!emit_event(mouse_fd, EV_KEY, button, button_value != 0 ? 1 : 0, error)) {
            close_locked();
            return "virtual mouse button failed: " + error;
        }
        pressed_button = button_value != 0 ? button : 0;
    }
    if (!emit_event(mouse_fd, EV_SYN, SYN_REPORT, 0, error)) {
        close_locked();
        return "virtual mouse sync failed: " + error;
    }
    return "ok: virtual mouse frame";
}

std::string release_mouse() {
    std::lock_guard<std::mutex> guard(mouse_lock);
    close_locked();
    return "ok: virtual mouse released";
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_openVirtualMouse(JNIEnv* env, jclass) {
    std::string result = open_mouse();
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_emitVirtualMouseFrame(
        JNIEnv* env, jclass, jint dx, jint dy, jint wheel, jint button, jint button_value) {
    std::string result = emit_frame(dx, dy, wheel, button, button_value);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mastercook777_heimdall_UinputNativeProbe_releaseVirtualMouse(JNIEnv* env, jclass) {
    std::string result = release_mouse();
    return env->NewStringUTF(result.c_str());
}
