package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 *
 * Created by Dr. Failov on 29.09.2017.
 */

public class MathSolver extends Function {
    public MathSolver(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);
        try {
            String ex = message.getText().toLowerCase();
            ex = ex.replace("ты ", "");
            ex = ex.replace("мне ", "");
            ex = ex.replace("ка ", "");
            ex = ex.replace("сколько будет", "");
            ex = ex.replace("реши мне", "");
            ex = ex.replace("реши", "");
            ex = ex.replace("сколько", "");
            ex = ex.replace("можешь решить", "");
            ex = ex.replace("cможешь решить", "");
            ex = ex.replace("посчитай", "");
            ex = ex.replace("вычисли", "");
            ex = ex.replace("помоги", "");
            ex = ex.replace("поможешь", "");
            ex = ex.replace(":", "");
            ex = ex.replace("?", "");
            ex = ex.replace("равно", "");
            ex = ex.replace("равняется", "");
            ex = ex.replace("=", "");

            ex = ex.replace("одиннадцать", "11");
            ex = ex.replace("двенадцать", "12");
            ex = ex.replace("тринадцать", "13");
            ex = ex.replace("четырнадцать", "14");
            ex = ex.replace("пятнадцать", "15");
            ex = ex.replace("шестнадцать", "16");
            ex = ex.replace("восемнадцать", "18"); //во17
            ex = ex.replace("семнадцать", "17");
            ex = ex.replace("девятнадцать", "19");

            ex = ex.replace("двадцать", "20+0");
            ex = ex.replace("тридцать", "30+0");
            ex = ex.replace("сорок", "40+0");
            ex = ex.replace("пятдесят", "50+0");
            ex = ex.replace("шестьдесят", "60+0");
            ex = ex.replace("восемьдесят", "80+0");
            ex = ex.replace("семьдесят", "70+0");
            ex = ex.replace("девяносто", "90+0");
            ex = ex.replace("сто", "100+0");
            ex = ex.replace("двести", "200+0");
            ex = ex.replace("триста", "300+0");
            ex = ex.replace("четыреста", "400+0");
            ex = ex.replace("пятьсот", "500+0");
            ex = ex.replace("шестьсот", "600+0");
            ex = ex.replace("семьсот", "700+0");
            ex = ex.replace("восемьсот", "800+0");
            ex = ex.replace("девятьсот", "900+0");
            ex = ex.replace("тысячу", "1000+0");
            ex = ex.replace("тысяча", "1000+0");
            ex = ex.replace("тысячи", "*1000+0");
            ex = ex.replace("тысяч", "*1000+0");

            ex = ex.replace("один", "1");
            ex = ex.replace("два", "2");
            ex = ex.replace("две", "2");
            ex = ex.replace("три", "3");
            ex = ex.replace("четыре", "4");
            ex = ex.replace("пять", "5");
            ex = ex.replace("шесть", "6");
            ex = ex.replace("восемь", "8");
            ex = ex.replace("семь", "7");
            ex = ex.replace("девять", "9");
            ex = ex.replace("десять", "10");
            ex = ex.replace("ноль", "0");

            ex = ex.replace("плюс", "+");

            ex = ex.replace("минус", "-");

            ex = ex.replace("разделить на", "/");
            ex = ex.replace("делить на", "/");
            ex = ex.replace("÷", "/");
            ex = ex.replace("\\", "/");

            ex = ex.replace("умножить на", "*");
            ex = ex.replace("множить на", "*");
            ex = ex.replace("умножить", "*");
            ex = ex.replace("x", "*");
            ex = ex.replace("х", "*");
            ex = ex.replace("×", "*");

            ex = ex.replace(",", ".");

            ex = ex.replace("в степени", "^");
            ex = ex.replace("степень", "^");
            ex = ex.replace("в квадрате", "^2");
            ex = ex.replace("²", "^2");
            ex = ex.replace("в кубе", "^3");
            ex = ex.replace("³", "^3");


            ex = ex.replace(" и ", "");
            ex = ex.replace("это все", "");
            ex = ex.replace("это всё", "");

            ex = ex.replace("пи", "pi");
            ex = ex.replace("pi", "3.14159265358979323846");

            ex = ex.replace("е", "e");
            ex = ex.replace("e", "2.71828182846");

            ex = ex.replace(" ", "");
//                log(ex);

            //проверить есть ли там вообще цифры
            if(!ex.contains("1")
                    &&!ex.contains("2")
                    &&!ex.contains("3")
                    &&!ex.contains("4")
                    &&!ex.contains("5")
                    &&!ex.contains("6")
                    &&!ex.contains("7")
                    &&!ex.contains("8")
                    &&!ex.contains("9")
                    &&!ex.contains("0")
                    &&!ex.contains("/")
                    &&!ex.contains("*")
                    &&!ex.contains("+")
                    &&!ex.contains("-"))
                return super.processMessage(messageOriginal);

            Expression expression = new ExpressionBuilder(ex).build();
            String result = "Результат: " + String.valueOf(expression.evaluate());
            result = result.replace('.', ',');
            result = result + ".";
            result = result.replace(",0.", ".");

            message.setAnswer(new Answer(result));
            message = prepare(message);
            return message;
        }
        catch (Exception e){
            return super.processMessage(messageOriginal);
        }
    }
    @Override
    public String getName() {
        return "math";
    }
    @Override
    public String getDescription() {
        return "Модуль перехватывает математические выражения и пытается решить их.";
    }
}
