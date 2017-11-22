package com.fsoft.vktest.Utils;

/**
 * ������� ����� � �����
 * Source: https://ru.stackoverflow.com/questions/449055/java-%D1%80%D0%B5%D0%B0%D0%BB%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D1%8F-%D1%81%D1%83%D0%BC%D0%BC%D1%8B-%D0%BF%D1%80%D0%BE%D0%BF%D0%B8%D1%81%D1%8C%D1%8E
 * Created by Dr. Failov on 19.09.2017.
 */

import java.util.Stack;

public class NumberToText {
    private static enum Ranges {UNITS, DECADES, HUNDREDS, THOUSANDS, MILLIONS, BILLIONS};
    private static Stack <ThreeChar> threeChars;

    private static class ThreeChar {
        char h, d, u;
        Ranges range;
    }

    public static String digits2Text(Double d){
        if(d == null || d < 0.0) return null;
        String s = d.toString();
        int n = s.length() - s.lastIndexOf('.');
        if(n > 3) return null;
        if(n == 2) s += "0";
        String[] sa = s.split("\\.");
        threeChars = new Stack <ThreeChar> ();
        threeChars.push(new ThreeChar());
        threeChars.peek().range = Ranges.UNITS;
        StringBuilder sb = new StringBuilder(sa[0]).reverse();
        for(int i = 0; i < sb.length(); i++){
            if(i > 0 && i % 3 == 0){
                threeChars.push(new ThreeChar());
            }
            ThreeChar threeChar = threeChars.peek();
            switch(i){
                case 0:
                    threeChar.u = sb.charAt(i);
                    break;
                case 3:
                    threeChar.range = Ranges.THOUSANDS;
                    threeChar.u = sb.charAt(i);
                    break;
                case 6:
                    threeChar.range = Ranges.MILLIONS;
                    threeChar.u = sb.charAt(i);
                    break;
                case 9:
                    threeChar.range = Ranges.BILLIONS;
                    threeChar.u = sb.charAt(i);
                    break;
                case 2:
                case 5:
                case 8:
                    threeChar.h = sb.charAt(i);
                    break;
                default:
                    threeChar.d = sb.charAt(i);
            }
        }
        StringBuilder result = new StringBuilder();
        while(!threeChars.isEmpty()){
            ThreeChar thch = threeChars.pop();
            if(thch.h > 0  ){
                result.append(getHundreds(thch.h));
                result.append(' ');
            }
            if(thch.d > '0'){
                if(thch.d > '1' || (thch.d == '1' && thch.u == '0')) result.append(getDecades(thch.d));
                else if(thch.d > '0') result.append(getTeens(thch.d));
                result.append(' ');
            }
            if(thch.u > '0' && thch.d != '1'){
                result.append(getUnits(thch.u, thch.range == Ranges.THOUSANDS));
                result.append(' ');
            }
            switch(thch.range){
                case BILLIONS:
                    if(thch.d == '1' || thch.u == '0') result.append("����������");
                    else if(thch.u > '4')result.append("����������");
                    else if(thch.u > '1')result.append("���������");
                    else result.append("��������");
                    break;
                case MILLIONS:
                    if(thch.d == '1' || thch.u == '0') result.append("���������");
                    else if(thch.u > '4')result.append("���������");
                    else if(thch.u > '1')result.append("��������");
                    else result.append("�������");
                    break;
                case THOUSANDS:
                    if(thch.d == '1' || thch.u == '0') result.append("�����");
                    else if(thch.u > '4')result.append("�����");
                    else if(thch.u > '1')result.append("������");
                    else result.append("������");
                    break;
                default:
                    if(thch.d == '1' || thch.u == '0' || thch.u > '4')result.append("������");
                    else if(thch.u > '1')result.append("�����");
                    else result.append("�����");
            }
            result.append(' ');
        }
        result.append(sa[1] + ' ');
        switch(sa[1].charAt(1)){
            case '1':
                result.append(sa[1].charAt(0) != '1' ? "�������" : "������");
                break;
            case '2':
            case '3':
            case '4':
                result.append(sa[1].charAt(0) != '1' ? "�������" : "������");
                break;
            default:
                result.append("������");
        }
        char first = Character.toUpperCase(result.charAt(0));
        result.setCharAt(0, first);
        return result.toString();
    }

    private static String getHundreds(char dig){
        switch(dig){
            case '1':
                return "���";
            case '2':
                return "������";
            case '3':
                return "������";
            case '4':
                return "���������";
            case '5':
                return "�������";
            case '6':
                return "�������";
            case '7':
                return "������";
            case '8':
                return "��������";
            case '9':
                return "���������";
            default: return null;
        }
    }
    private static String getDecades(char dig){
        switch(dig){
            case '1':
                return "������";
            case '2':
                return "��������";
            case '3':
                return "��������";
            case '4':
                return "�����";
            case '5':
                return "���������";
            case '6':
                return "����������";
            case '7':
                return "���������";
            case '8':
                return "�����������";
            case '9':
                return "���������";
            default: return null;
        }
    }
    private static String getUnits(char dig, boolean female){
        switch(dig){
            case '1':
                return female ? "����" : "����";
            case '2':
                return female ? "���"  : "���";
            case '3':
                return "���";
            case '4':
                return "������";
            case '5':
                return "����";
            case '6':
                return "�����";
            case '7':
                return "����";
            case '8':
                return "������";
            case '9':
                return "������";
            default: return null;
        }
    }
    private static String getTeens(char dig){
        String s = "";
        switch(dig){
            case '1':
                s = "����"; break;
            case '2':
                s = "���"; break;
            case '3':
                s = "���"; break;
            case '4':
                s = "�����"; break;
            case '5':
                s = "���"; break;
            case '6':
                s = "����"; break;
            case '7':
                s = "���"; break;
            case '8':
                s = "�����"; break;
            case '9':
                s = "�����"; break;
        }
        return s + "�������";
    }
}
