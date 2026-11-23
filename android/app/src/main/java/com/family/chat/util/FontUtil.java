package com.family.chat.util;

import android.text.TextUtils;
import android.widget.TextView;

public class FontUtil {

    /**
     * 👈 仿微信官方：将长文本进行物理截断，并将系统默认下沉的 "..." 替换为垂直居中的 "···"
     *
     * @param textView 需要进行文本限制的 TextView 控件
     * @param rawText  原始的长文本字符串
     */
    public static void setWeChatStyleEllipsize(final TextView textView, final String rawText) {
        if (textView == null || rawText == null) {
            return;
        }

        // 使用 post 机制，确保在系统完成布局测量、获取到 TextView 的真实物理宽度后再进行计算
        textView.post(new Runnable() {
            @Override
            public void run() {
                // 计算可用于显示文本的净宽度（扣除左右内边距）
                int availableWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();

                if (availableWidth > 0) {
                    CharSequence ellipsizedText = TextUtils.ellipsize(
                            rawText,
                            textView.getPaint(),
                            availableWidth,
                            TextUtils.TruncateAt.END
                    );

                    String finalXmlText = ellipsizedText.toString().replace("…", "···");
                    textView.setText(finalXmlText);
                } else {
                    textView.setText(rawText);
                }
            }
        });
    }
}