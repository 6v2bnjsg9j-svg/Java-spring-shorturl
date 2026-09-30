package com.wcy.shorturl.codec;

public final class Base62 {


    private static final  char[] base="0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int baseLength=base.length;
    private static final int[] DIGITS = new int[128];

    static {
        for(int i=0;i<base.length;i++)
            DIGITS[base[i]] = i;
    }

    public static String encode(long num) {
        if (num < 0) {
            throw new IllegalArgumentException("仅支持非负数: " + num);
        }
        if (num == 0) {
            return "0";
        }
        StringBuilder result = new StringBuilder();
        while (num != 0)
        {
            char c = (char) base[(int) (num % baseLength)];
            num /= baseLength;
            result.append(c);
        }
        return result.reverse().toString();
    }


    public static long decode(String str) {
        if(str==null||str.isBlank())
        {
            throw new IllegalArgumentException("仅支持非空字符串: " + str);
        }
        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if ( c >= 128 || (DIGITS[c] == 0 && c != '0')) {
                throw new IllegalArgumentException("非法短码字符: " + c);
            }
            int digit= DIGITS[c];
            if (result > (Long.MAX_VALUE - digit) / baseLength) {
                throw new IllegalArgumentException("短码超出范围: " + str);
            }
            result = result * baseLength + DIGITS[c];
        }
        return result;
    }
}

