package com.mastercook777.heimdall;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Owns one Activity's key state and reference-counts shared chord modifiers. */
final class KeyboardInputSession {
    interface Listener {
        void onReady();
        void onUnavailable();
    }

    private final Context context;
    private final Listener listener;
    private final Map<Integer, Integer> pressedCodeCounts = new HashMap<>();
    private final IdentityHashMap<Object, List<Integer>> activeHolds = new IdentityHashMap<>();
    private VirtualKeyboardDispatcher dispatcher;

    KeyboardInputSession(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    void prepare() {
        ensureDispatcher();
    }

    void press(KeyboardPad.Binding binding) {
        List<Integer> codes = codesFor(binding);
        for (int code : codes) {
            retainCode(code);
        }
        for (int i = codes.size() - 1; i >= 0; i--) {
            releaseCode(codes.get(i));
        }
    }

    void hold(Object token, KeyboardPad.Binding binding) {
        if (token == null || activeHolds.containsKey(token)) {
            return;
        }
        List<Integer> codes = codesFor(binding);
        activeHolds.put(token, codes);
        for (int code : codes) {
            retainCode(code);
        }
    }

    void holdKey(Object token, int linuxKeyCode) {
        if (token == null || activeHolds.containsKey(token)
                || !VirtualKeyboardDispatcher.isSupportedKeyCode(linuxKeyCode)) {
            return;
        }
        List<Integer> codes = Collections.singletonList(linuxKeyCode);
        activeHolds.put(token, codes);
        retainCode(linuxKeyCode);
    }

    void release(Object token) {
        List<Integer> codes = activeHolds.remove(token);
        if (codes == null) {
            return;
        }
        for (int i = codes.size() - 1; i >= 0; i--) {
            releaseCode(codes.get(i));
        }
    }

    void releaseAll() {
        pressedCodeCounts.clear();
        activeHolds.clear();
        if (dispatcher != null) {
            dispatcher.releaseAll();
        }
    }

    void park() {
        pressedCodeCounts.clear();
        activeHolds.clear();
        VirtualKeyboardDispatcher active = dispatcher;
        dispatcher = null;
        if (active != null) {
            active.park();
        }
    }

    void close() {
        pressedCodeCounts.clear();
        activeHolds.clear();
        VirtualKeyboardDispatcher active = dispatcher;
        dispatcher = null;
        if (active != null) {
            active.close();
        } else {
            VirtualKeyboardDispatcher.destroyParkedDevice(context);
        }
    }

    private void retainCode(int code) {
        int count = pressedCodeCounts.containsKey(code) ? pressedCodeCounts.get(code) : 0;
        pressedCodeCounts.put(code, count + 1);
        if (count == 0) {
            ensureDispatcher().key(code, true);
        }
    }

    private void releaseCode(int code) {
        Integer count = pressedCodeCounts.get(code);
        if (count == null) {
            return;
        }
        if (count > 1) {
            pressedCodeCounts.put(code, count - 1);
            return;
        }
        pressedCodeCounts.remove(code);
        if (dispatcher != null) {
            dispatcher.key(code, false);
        }
    }

    private VirtualKeyboardDispatcher ensureDispatcher() {
        if (dispatcher == null) {
            dispatcher = new VirtualKeyboardDispatcher(context,
                    new VirtualKeyboardDispatcher.Listener() {
                        @Override
                        public void onReady() {
                            listener.onReady();
                        }

                        @Override
                        public void onUnavailable() {
                            handleUnavailable();
                        }
                    });
            dispatcher.start();
        }
        return dispatcher;
    }

    private void handleUnavailable() {
        pressedCodeCounts.clear();
        activeHolds.clear();
        VirtualKeyboardDispatcher failed = dispatcher;
        dispatcher = null;
        if (failed != null) {
            failed.close();
        }
        listener.onUnavailable();
    }

    private static List<Integer> codesFor(KeyboardPad.Binding binding) {
        if (binding == null || !VirtualKeyboardDispatcher.isSupportedKeyCode(
                binding.linuxKeyCode)) {
            return Collections.emptyList();
        }
        LinkedHashSet<Integer> codes = new LinkedHashSet<>();
        if (binding.ctrl) codes.add(KeyboardKeyCatalog.KEY_LEFTCTRL);
        if (binding.shift) codes.add(KeyboardKeyCatalog.KEY_LEFTSHIFT);
        if (binding.alt) codes.add(KeyboardKeyCatalog.KEY_LEFTALT);
        if (binding.win) codes.add(KeyboardKeyCatalog.KEY_LEFTMETA);
        codes.add(binding.linuxKeyCode);
        return new ArrayList<>(codes);
    }
}
