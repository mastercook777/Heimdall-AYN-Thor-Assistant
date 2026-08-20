package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class KeyboardKeyCatalog {
    static final int KEY_ESC = 1;
    static final int KEY_W = 17;
    static final int KEY_ENTER = 28;
    static final int KEY_LEFTCTRL = 29;
    static final int KEY_A = 30;
    static final int KEY_S = 31;
    static final int KEY_D = 32;
    static final int KEY_LEFTSHIFT = 42;
    static final int KEY_LEFTALT = 56;
    static final int KEY_SPACE = 57;
    static final int KEY_F1 = 59;
    static final int KEY_LEFTMETA = 125;

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
