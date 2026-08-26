package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class KeyboardKeyCatalog {
    static final int KEY_ESC = 1;
    static final int KEY_1 = 2;
    static final int KEY_2 = 3;
    static final int KEY_3 = 4;
    static final int KEY_4 = 5;
    static final int KEY_5 = 6;
    static final int KEY_6 = 7;
    static final int KEY_7 = 8;
    static final int KEY_8 = 9;
    static final int KEY_9 = 10;
    static final int KEY_0 = 11;
    static final int KEY_MINUS = 12;
    static final int KEY_EQUAL = 13;
    static final int KEY_BACKSPACE = 14;
    static final int KEY_TAB = 15;
    static final int KEY_Q = 16;
    static final int KEY_W = 17;
    static final int KEY_E = 18;
    static final int KEY_R = 19;
    static final int KEY_T = 20;
    static final int KEY_Y = 21;
    static final int KEY_U = 22;
    static final int KEY_I = 23;
    static final int KEY_O = 24;
    static final int KEY_P = 25;
    static final int KEY_LEFTBRACE = 26;
    static final int KEY_RIGHTBRACE = 27;
    static final int KEY_ENTER = 28;
    static final int KEY_LEFTCTRL = 29;
    static final int KEY_A = 30;
    static final int KEY_S = 31;
    static final int KEY_D = 32;
    static final int KEY_F = 33;
    static final int KEY_G = 34;
    static final int KEY_H = 35;
    static final int KEY_J = 36;
    static final int KEY_K = 37;
    static final int KEY_L = 38;
    static final int KEY_SEMICOLON = 39;
    static final int KEY_APOSTROPHE = 40;
    static final int KEY_GRAVE = 41;
    static final int KEY_LEFTSHIFT = 42;
    static final int KEY_BACKSLASH = 43;
    static final int KEY_Z = 44;
    static final int KEY_X = 45;
    static final int KEY_C = 46;
    static final int KEY_V = 47;
    static final int KEY_B = 48;
    static final int KEY_N = 49;
    static final int KEY_M = 50;
    static final int KEY_COMMA = 51;
    static final int KEY_DOT = 52;
    static final int KEY_SLASH = 53;
    static final int KEY_RIGHTSHIFT = 54;
    static final int KEY_KPASTERISK = 55;
    static final int KEY_LEFTALT = 56;
    static final int KEY_SPACE = 57;
    static final int KEY_CAPSLOCK = 58;
    static final int KEY_F1 = 59;
    static final int KEY_F2 = 60;
    static final int KEY_F3 = 61;
    static final int KEY_F4 = 62;
    static final int KEY_F5 = 63;
    static final int KEY_F6 = 64;
    static final int KEY_F7 = 65;
    static final int KEY_F8 = 66;
    static final int KEY_F9 = 67;
    static final int KEY_F10 = 68;
    static final int KEY_NUMLOCK = 69;
    static final int KEY_KP7 = 71;
    static final int KEY_KP8 = 72;
    static final int KEY_KP9 = 73;
    static final int KEY_KPMINUS = 74;
    static final int KEY_KP4 = 75;
    static final int KEY_KP5 = 76;
    static final int KEY_KP6 = 77;
    static final int KEY_KPPLUS = 78;
    static final int KEY_KP1 = 79;
    static final int KEY_KP2 = 80;
    static final int KEY_KP3 = 81;
    static final int KEY_KP0 = 82;
    static final int KEY_KPDOT = 83;
    static final int KEY_F11 = 87;
    static final int KEY_F12 = 88;
    static final int KEY_KPENTER = 96;
    static final int KEY_RIGHTCTRL = 97;
    static final int KEY_KPSLASH = 98;
    static final int KEY_RIGHTALT = 100;
    static final int KEY_HOME = 102;
    static final int KEY_UP = 103;
    static final int KEY_PAGEUP = 104;
    static final int KEY_LEFT = 105;
    static final int KEY_RIGHT = 106;
    static final int KEY_END = 107;
    static final int KEY_DOWN = 108;
    static final int KEY_PAGEDOWN = 109;
    static final int KEY_INSERT = 110;
    static final int KEY_DELETE = 111;
    static final int KEY_LEFTMETA = 125;
    static final int KEY_RIGHTMETA = 126;

    static final class Option {
        final int linuxKeyCode;
        final String label;

        Option(int linuxKeyCode, String label) {
            this.linuxKeyCode = linuxKeyCode;
            this.label = label;
        }
    }

    private static final List<Option> OPTIONS = createOptions();

    static List<Option> options() {
        return OPTIONS;
    }

    static String labelForCode(int code) {
        for (Option option : OPTIONS) {
            if (option.linuxKeyCode == code) {
                return option.label;
            }
        }
        return "KEY " + code;
    }

    static String bindingSummary(KeyboardPad.Binding binding) {
        if (binding == null) {
            return labelForCode(KEY_A);
        }
        List<String> parts = new ArrayList<>();
        if (binding.ctrl && binding.linuxKeyCode != KEY_LEFTCTRL) parts.add("Ctrl");
        if (binding.shift && binding.linuxKeyCode != KEY_LEFTSHIFT) parts.add("Shift");
        if (binding.alt && binding.linuxKeyCode != KEY_LEFTALT) parts.add("Alt");
        if (binding.win && binding.linuxKeyCode != KEY_LEFTMETA) parts.add("Win");
        parts.add(labelForCode(binding.linuxKeyCode));
        StringBuilder summary = new StringBuilder();
        for (String part : parts) {
            if (summary.length() > 0) summary.append(" + ");
            summary.append(part);
        }
        return summary.toString();
    }

    private static List<Option> createOptions() {
        List<Option> options = new ArrayList<>();
        options.add(new Option(KEY_ESC, "Esc"));
        String[] numberLabels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        for (int i = 0; i < numberLabels.length; i++) {
            options.add(new Option(2 + i, numberLabels[i]));
        }
        options.add(new Option(14, "Backspace"));
        options.add(new Option(15, "Tab"));
        addLetters(options, "QWERTYUIOP", 16);
        options.add(new Option(KEY_ENTER, "Enter"));
        options.add(new Option(KEY_LEFTCTRL, "Ctrl"));
        addLetters(options, "ASDFGHJKL", 30);
        options.add(new Option(KEY_LEFTSHIFT, "Shift"));
        addLetters(options, "ZXCVBNM", 44);
        options.add(new Option(51, ","));
        options.add(new Option(52, "."));
        options.add(new Option(53, "/"));
        options.add(new Option(KEY_LEFTALT, "Alt"));
        options.add(new Option(KEY_SPACE, "Space"));
        for (int i = 0; i < 10; i++) {
            options.add(new Option(59 + i, "F" + (i + 1)));
        }
        options.add(new Option(87, "F11"));
        options.add(new Option(88, "F12"));
        options.add(new Option(102, "Home"));
        options.add(new Option(103, "Up"));
        options.add(new Option(104, "Page Up"));
        options.add(new Option(105, "Left"));
        options.add(new Option(106, "Right"));
        options.add(new Option(107, "End"));
        options.add(new Option(108, "Down"));
        options.add(new Option(109, "Page Down"));
        options.add(new Option(110, "Insert"));
        options.add(new Option(111, "Delete"));
        options.add(new Option(KEY_LEFTMETA, "Win"));
        return Collections.unmodifiableList(options);
    }

    private static void addLetters(List<Option> options, String letters, int firstCode) {
        for (int i = 0; i < letters.length(); i++) {
            options.add(new Option(firstCode + i, String.valueOf(letters.charAt(i))));
        }
    }

    private KeyboardKeyCatalog() {}
}
