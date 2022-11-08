package com.ruiyun.jvppeteer.enums;

import lombok.AllArgsConstructor;

/**
 * 键盘按键， 参考{@link com.ruiyun.jvppeteer.core.page.Keyboard}
 */
@AllArgsConstructor
public enum KeyCodeEnum {
    Enter("Enter"),
    Backspace("Backspace"),
    Delete("Delete"),
    Home("Home"),
    End("End"),
    Esc("Escape"),
    Control("Control"),
    V("V"),
    ;

    public final String code;
}
