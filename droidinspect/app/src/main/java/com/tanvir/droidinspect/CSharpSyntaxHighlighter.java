package com.tanvir.droidinspect;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight, offline C# lexical highlighting for the phone-sized source editor. */
final class CSharpSyntaxHighlighter {
    private static final Pattern TOKEN = Pattern.compile(
            "//[^\\n]*|/\\*[\\s\\S]*?\\*/|@?\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\\b(?:abstract|as|async|await|base|bool|break|byte|case|catch|char|checked|class|const|continue|decimal|default|delegate|do|double|else|enum|event|explicit|extern|false|finally|fixed|float|for|foreach|goto|if|implicit|in|int|interface|internal|is|lock|long|namespace|new|null|object|operator|out|override|params|private|protected|public|readonly|ref|return|sbyte|sealed|short|sizeof|stackalloc|static|string|struct|switch|this|throw|true|try|typeof|uint|ulong|unchecked|unsafe|ushort|using|var|virtual|void|volatile|while|record|init|global|file|with|yield)\\b|\\b\\d+(?:\\.\\d+)?[uUlLfFdDmM]*\\b",
            Pattern.MULTILINE);

    private CSharpSyntaxHighlighter() { }

    static void apply(Editable text) {
        if (text == null) return;
        ForegroundColorSpan[] colors = text.getSpans(0, text.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : colors) text.removeSpan(span);
        StyleSpan[] styles = text.getSpans(0, text.length(), StyleSpan.class);
        for (StyleSpan span : styles) text.removeSpan(span);

        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            int color;
            boolean bold = false;
            if (token.startsWith("//") || token.startsWith("/*")) {
                color = Color.rgb(120, 155, 128);
            } else if (token.startsWith("\"") || token.startsWith("@\"") || token.startsWith("'")) {
                color = Color.rgb(214, 160, 110);
            } else if (Character.isDigit(token.charAt(0))) {
                color = Color.rgb(181, 205, 145);
            } else {
                color = Color.rgb(130, 175, 235);
                bold = true;
            }
            text.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (bold) text.setSpan(new StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
