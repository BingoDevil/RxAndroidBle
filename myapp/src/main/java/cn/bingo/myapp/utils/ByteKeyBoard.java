package cn.bingo.myapp.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import cn.bingo.myapp.R;

/**
 * Created by Bingo on 16/8/16
 */
public class ByteKeyBoard {

    private static PopupWindow pop;
    private static TextView textValues;

    private static String values = "";

    /**
     * 设置pop的布局样式已及动画
     *
     * @param v
     */
    public static void ShowKeyboard(PopupWindow popupWindow, View v, Context context, View positionView, TextView textView) {
        if (v != null) {
            textValues = textView;
            pop = popupWindow;
            pop = new PopupWindow(v);
            pop.setFocusable(true); // 加上这个内部才可以接收点击事件
            // 设置PopupWindow的宽高
            pop.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            pop.setHeight(MyPixle.dip2px(context, 256)); // 256dp 这里要的是PX
            // 设置加载动画
            pop.setAnimationStyle(R.style.AnimBottom);
            // 为PopupWindow设置背景图片，必须在代码块设置，不能在布局文件设置
            pop.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.ic_launcher));
            // 点击PopupWindow以外的地方PopupWindow消失
            pop.setOutsideTouchable(true);
            // 第一个参数此控件不起作用,仅仅是提供给pop作为参考物
            pop.showAtLocation(positionView, Gravity.RIGHT | Gravity.BOTTOM, 0, 0);
            popinit(v); // 初始化pop里面的控件
        }
    }

    private static void popinit(View view) {
        BtnClickLis btnClickLis = new BtnClickLis();
        Button btn_ok = (Button) view.findViewById(R.id.btn_ok);
        Button btn_d = (Button) view.findViewById(R.id.btn_d);
        Button btn_e = (Button) view.findViewById(R.id.btn_e);
        Button btn_f = (Button) view.findViewById(R.id.btn_f);
        Button btn_a = (Button) view.findViewById(R.id.btn_a);
        Button btn_b = (Button) view.findViewById(R.id.btn_b);
        Button btn_c = (Button) view.findViewById(R.id.btn_c);
        Button btn_1 = (Button) view.findViewById(R.id.btn_1);
        Button btn_2 = (Button) view.findViewById(R.id.btn_2);
        Button btn_3 = (Button) view.findViewById(R.id.btn_3);
        Button btn_4 = (Button) view.findViewById(R.id.btn_4);
        Button btn_5 = (Button) view.findViewById(R.id.btn_5);
        Button btn_6 = (Button) view.findViewById(R.id.btn_6);
        Button btn_7 = (Button) view.findViewById(R.id.btn_7);
        Button btn_8 = (Button) view.findViewById(R.id.btn_8);
        Button btn_9 = (Button) view.findViewById(R.id.btn_9);
        Button btn_0 = (Button) view.findViewById(R.id.btn_0);
        Button btn_del = (Button) view.findViewById(R.id.btn_del);

        btn_ok.setOnClickListener(btnClickLis);
        btn_d.setOnClickListener(btnClickLis);
        btn_e.setOnClickListener(btnClickLis);
        btn_f.setOnClickListener(btnClickLis);
        btn_a.setOnClickListener(btnClickLis);
        btn_b.setOnClickListener(btnClickLis);
        btn_c.setOnClickListener(btnClickLis);
        btn_1.setOnClickListener(btnClickLis);
        btn_2.setOnClickListener(btnClickLis);
        btn_3.setOnClickListener(btnClickLis);
        btn_4.setOnClickListener(btnClickLis);
        btn_5.setOnClickListener(btnClickLis);
        btn_6.setOnClickListener(btnClickLis);
        btn_7.setOnClickListener(btnClickLis);
        btn_8.setOnClickListener(btnClickLis);
        btn_9.setOnClickListener(btnClickLis);
        btn_0.setOnClickListener(btnClickLis);
        btn_del.setOnClickListener(btnClickLis);
    }

    static class BtnClickLis implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_ok:
                    pop.dismiss();
                    break;
                case R.id.btn_d:
                    textValues.setText(values += "D");
                    break;
                case R.id.btn_e:
                    textValues.setText(values += "E");
                    break;
                case R.id.btn_f:
                    textValues.setText(values += "F");
                    break;
                case R.id.btn_a:
                    textValues.setText(values += "A");
                    break;
                case R.id.btn_b:
                    textValues.setText(values += "B");
                    break;
                case R.id.btn_c:
                    textValues.setText(values += "C");
                    break;
                case R.id.btn_1:
                    textValues.setText(values += "1");
                    break;
                case R.id.btn_2:
                    textValues.setText(values += "2");
                    break;
                case R.id.btn_3:
                    textValues.setText(values += "3");
                    break;
                case R.id.btn_4:
                    textValues.setText(values += "4");
                    break;
                case R.id.btn_5:
                    textValues.setText(values += "5");
                    break;
                case R.id.btn_6:
                    textValues.setText(values += "6");
                    break;
                case R.id.btn_7:
                    textValues.setText(values += "7");
                    break;
                case R.id.btn_8:
                    textValues.setText(values += "8");
                    break;
                case R.id.btn_9:
                    textValues.setText(values += "9");
                    break;
                case R.id.btn_0:
                    textValues.setText(values += "0");
                    break;
                case R.id.btn_del:
                    values = "";
                    textValues.setText(values);
                    pop.dismiss();
                    break;
            }
        }
    }

}
