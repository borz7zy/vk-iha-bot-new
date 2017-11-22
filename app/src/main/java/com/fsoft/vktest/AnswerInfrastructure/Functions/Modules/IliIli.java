package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Utils.Parameters;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * выбор промеж двух вариантов
 * Created by Dr. Failov on 14.02.2017.
 */
public class IliIli extends Function {
    public IliIli(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        String text = message.getText();

        //// TODO: 26.09.2017 не понижает ли подобная конструкция производительность драматически?
        String[] texts = new String[]{
                "Скорее всего, SEL.",
                "Кажется, SEL.",
                "Вариант SEL кажется логичным.",
                "Мне кажется, что SEL.",
                "Думаю, SEL.",
                "Что-то мне подсказывает, что SEL.",
                "Я бы тебе посоветовал SEL.",
                "SEL."
        };
        texts = applicationManager.getParameters().get("iliili_patterns",
                texts,
                "Список вариантов ответа модуля или-или",
                "Бот может отвечать на вопросы типа \"Чай или кофе?\" выбирая ответ случайным образом.\n" +
                        "При ответе бот случайным образом выбирает один из этих шаблонов.\n" +
                        "Фрагмент SEL в шаблоне заменяется на вариант, который бот выбрал.");

        if(text.contains(" или ") && text.contains("?")){
            //отфильтровать текст

            //что выпить: чай или кофе или цикорий?
            //что выпить, чай или кофе или цикорий?
            //чай или кофе или цикорий?
            //
            //чай или кофе или цикорий

            String mydata = text;
            Pattern pattern = Pattern.compile("(.+[:,] ?)?(.+ или .+)\\?");
            Matcher matcher = pattern.matcher(mydata);
            if (matcher.find())
                text = matcher.group(2);

            //проверить длину
            if(text.length() > 500)
                return super.processMessage(messageOriginal);
            Random random = new Random(System.currentTimeMillis());
            //разделить
            String[] tmp = text.split(" или ");
            //фильтовать от пустых строк
            int cnt = 0;
            for (String s:tmp)
                if (s != null && !s.equals(""))
                    cnt ++;
            String[] vars = new String[cnt];
            int ptr = 0;
            for (String s:tmp)
                if (s != null && !s.equals("")){
                    vars[ptr] = s;
                    ptr ++;
                }
            //проверка на пустой варивант
            log("vars = " + vars.length);
            if(vars.length < 2){
                message.setAnswer(new Answer("или что?"));
                message = prepare(message);
                return message;
            }
            //--------------случайный выбор
            int var = random.nextInt(vars.length);
            String selection = vars[var].trim();
            //отправка ответа
            String result = texts[random.nextInt(texts.length)];
            message.setAnswer(new Answer(result.replace("SEL", selection)));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }

    @Override public String getName() {
        return "oror";
    }
    @Override public String getDescription() {
        return "Выбор случайного варианта в конструкции \"... или ... ?\".\n" +
                "Также сработает конструкция типа \"Что выпить, чай или кофе?\"\n" +
                "Знак вопроса в конце обязательный.";
    }
}
