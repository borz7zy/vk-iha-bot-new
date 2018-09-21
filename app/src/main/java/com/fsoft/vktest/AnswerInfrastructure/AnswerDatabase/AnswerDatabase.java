package com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase;


import android.content.res.Resources;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.BotModule;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.AnswerInfrastructure.MessageComparison.JaroWinkler;
import com.fsoft.vktest.AnswerInfrastructure.MessageComparison.MessagePreparer;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.VK.VkAccount;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.*;
import com.fsoft.vktest.Utils.CommandParser;
import com.perm.kate.api.Document;


import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/*
 * база данных ключевых слов и ответов на них
 * Created by Dr. Failov on 06.08.2014.
 */

/**
 * ----=== Полный пиздец. ===----
 * Приоритеты:
 * - много ответов
 * - большая скорость
 * - много инфы про ответ
 * - мало памяти
 * - обратная совместимость
 * -------------------------------------------------------------------------------------------------
 * Формат хранения базы: текстовый документ, каждая строка которого есть JSON с одним ответом
 * Какая инфа про ответ должна хранится:
 * - ID ответа (id, long. В случае коллизий генерировать новые)
 * - дата внесения ответа в базу (created_date, yyyy-MM-dd_HH:mm)
 * - дата редактирования (edited_date, yyyy-MM-dd_HH:mm)
 * - автор внесения ответа (created_author, vk ID)
 * - автор редактирования ответа (edited_author, vk ID)
 * - текст вопроса (question_text, текст экранирован по правилам JSON)
 * - текст ответа (answer_text, текст экранирован по правилам JSON)
 * - список вложений в ответе (answer_attachments, массив JsonObject's)
 * - набор вложений в вопросе (question_attachments, 1P0V0D0M)
 * - пол бота (bot_gender, М\Ж\-)
 * - пол собеседника (user_gender, М\Ж\-)
 * -------------------------------------------------------------------------------------------------
 * В оперативной памяти будут хранится:
 * - ID ответа
 * - искаженная версия вопроса
 * - ответ
 * - вложения в ответе
 * - вложения в вопросе
 * - пол собеседника
 * -------------------------------------------------------------------------------------------------
 * Внешний интерфейс класса:
 * - конструктор, загружает базу в оперативную память (AnswerTiny)
 * - getAnswer(), подбирает ответ из базы (Message.Answer)
 * - getById(list[]), шерстит файл в поисках нужных ответов (Answer)
 * - getById(), шерстит файл в поисках нужного ответа (Answer)
 * - getByAuthor(), шерстит файл в поисках нужного ответа (Answer[])
 * - getByAnswer(), шерстит файл в поисках нужного ответа (Answer[])
 * - AddAnswer(), шерстит файл в поисках такого же ID, такого же ответа, такого же вопроса. Если нету, добавляет
 *      новый ответ. Если такой ID уже есть, но нету такого ответа, генерирует новый ID
 * - getMaxId(), выдает максимальный ID из базы, чтобы можно было сгенерировать новый уникальный
 * - remAnswer(), по ID находит и удаляет ответ из файла и из оперативки.
 * - remAnswers(list[]), по ID находит и удаляет ответы из файла и из оперативки.
 * - editAnswer(), обновляет все поля ответа кроме ID в файле и в оперативке
 * -------------------------------------------------------------------------------------------------
 * Какой формат должна иметь база:
 *    Каждая строка - это JSON. Каждый JSON обязательно содержит ID.
 *    Все ответы должны быть строго в порядке возростания ID. Пропуски ID допускаются.
 *      Если последовательность будет нарушена, программа выполнит фильтрацию.
 *      Фильтрация длится относительно долго. Дольше обычной загрузки программы.
 *      Фильтрация происходит в несколько этапов.
 *      Фильтрация не требует дополнительных затрат оперативной памяти.
 *      Фильтрация заменит ID для всех ответов после места нарушения.
 *      Фильтрация также очистит дубликаты ответов.
 *    Если строка имеет не JSON формат - эта строка будет пропущена
 *      при загрузке и удалена из базы в момент любого изменения или фильтрации базы.
 *      При загрузке программы пользователь получит отчёт, если в базе будут выявлены нарущения.
 *    Все ответы и вопросы сохраняются в базе в их изначальном виде, без искажений
 *    Если в базе есть два одинаковых ответа - они будут уничтожены при фильтрации.
 *    Переход строки в оответы добавляется по правилам JSON, фрагмент " -n " больше не используется
 *    Вопросы могут содержать и цифры и латиницу. Их бот теперь тоже учитываем при подборе ответа.
 * -------------------------------------------------------------------------------------------------
 * (КОНСТРУКТОР) Как загружать базу для подготовки ответов на фразы:
 * - Открываем файл
 * - Считываем файл построчно
     * - Каждую строку считываем как JSON
     * - Парсим строку как AnswerMicroElement
     * - Сравниваем текущий ID с предыдущим (last). Он ДОЛЖЕН быть больше. Если он не больше - значит
        * последовательность нарушена и требуется фильтрация базы.
        * Тогда запускаем фильтрацию и стираем всю загруженную базу.
     * - Текст вопроса преображаем перед загрузкой ответа. Как преображать - предмет отдельной инструкции
     * - Сохраняем текуший ID как предыдущий (last = id)
     * - Вопрос вносим в массив
 * - Закрываем файл
 * -------------------------------------------------------------------------------------------------
 * (FILTER) Как фильтровать базу?
 * Если при запуске обнаружилось, что последовательность ответов нарушена, значит база может содержать
 * коллизии ID и нарушеную сортировку. А также Дубликаты ответов. Чтобы исправить все эти проблемы и нужна
 * фильтрация.
 * Фильтрация в неспадающем порядке позволит:
 * - Контролировать целостность базы
 * - Гарантировать отсутствие коллизий
 * - Ускорить многие алгоритмы
 * После фильтрации нужно запустить загрузку базы заново.
 * Итого, последовательность фильтрации:
 * ----------------- Очистка дубликатов
 * --- Этап 1: Поиск дубликатов
 * - Ищем в базе фразы, ответов на которые много.
 *      считываем базу построчно. Нас интересует для начала только поле question.
 *      Для него в оперативке хватит места. Вносим вопросы в массив loadedQuestions.
 *      При считывании каждого вопроса проверяем наличие этого вопроса в списке.
 *      Если этот вопрос уже есть в списке loadedQuestions, вносим этот вопрос в список repeatedQuestions.
 *      Если этого вопроса в списке нет, добавляем его в loadedQuestions.
 * --- этап 2, фильтрация дубликатов
 * - Открываем файл answer_database.tmp для запипи
 * - Загружаем из базы список тех ответов, у которых повторяются вопросы. Те что не повторяются - пишем в новый файл.
 *      Делаем второй проход по базе. На этот раз парсим полноценные AnswerElement.
 *      Если вопрос из ответа содержится в repeatedQuestions, вносим этот ответ В массив repeatedAnswers (таких мало)
 *      Если не содержится (таких большинство), записываем этот вопрос в answer_database.tmp;
 * - Фильтруем список повторяющихся ответов от дубликатов
 *      Делаем проход по списку повторяющихся вопросов (repeatedQuestions).
 *      Каждый вопрос из этого списка сравниваем с каждым вопросом из оставшегося этого списка
 *      Если находим полностью одинаковую пару, оставляем ответ, который старше.
 *      Делаем столько проходов, сколько потребуется чтобы по итогу было удалено 0 ответов.
 * - Дополняем новый файл оставшимися ответами
 *      Всё что осталось в repeatedQuestions записываем в answer_database.tmp
 * - Закрываем файл answer_database.tmp. Очистка дубликатов завершена!
 *--- Этап 3: "сортировка" IDшек ответов. (не совсем)
 * - fixing = false
 * - last = -1;
 * - Открываем файл answer_database.tmp1 для записи
 * - считываем answer_database.tmp построчно. Парсим AnswerElememt полностью
 *      - Если флажок fixing true тогда
 *            - id = last + 1;
 *            - Записываем этот ответ в answer_database.tmp1;
 *            - last ++;
 *      - Если флажок fixing false тогда
     *      Проверрить не нарушена ли последовательность. Если нарушена - начать исправляять.
     *      - Сравниваем id с last. Он должен быть больше.
     *      - Если ID меньше или равен, ставим флажок fixing = true, текущему элементу делаем id = last + 1;
 *          - Записываем элемент в answer_database.tmp1
 * - закоываем файл answer_database.tmp1
 * --- Этап 4, меняем файлы местами
 * -------------------------------------------------------------------------------------------------
 *  (ADD) Как добавлять ответ в базу:
 * - Открываем файл с базой: answer_database.bin
 * - Открываем временный файл с базой: answer_database.tmp
 * - Считываем файл с базой построчно
 *      - Каждую строку парсим как JSON
 *      - Каждый JSON парсим как AnswerElement
 *      - Проверяем вопрос. Если он совпадает с новым:
 *          - Проверяем ответ. Если он совпадает с новым, закрываем оба файла, сообщаем пользователю.
 *      - Из элемента достаем ID и сохраняем у себя максимальный ID
 *      - Записываем ответ во временный файл
 * - Закрываем файл с базой
 * - Определяем для нового ответа ID как max+1
 * - Получаем строку JSON для нового ответа
 * - Генерируем AnswerMicroElement для нового ответа
 * - Вносим AnswerMicroElement в оперативку
 * - Записываем новый ответ во временный файл
 * - Закрываем временный файл с базой
 * - Переносим файл с базой: answer_database.bin в папку sdcard\backups\database\backup_yyyy-MM-dd_HH-mm.bin
 * - Чистим кэш. Оставляем только (database_backups, 50) последних бекапов
 * - Переименовываем файл answer_database.tmp в answer_database.bin
 * -------------------------------------------------------------------------------------------------
 * (GET_ANSWER) Как подобрать ответ:
 * ". Прррррриветик [id666|Альфред]. как делишки?! :))) {]]}╚ "
 *
 * + Привести всё в нижний регистр
 * ". прррррриветик  [id666|альфред]. как, делишки?! :))) {]]}╚ "
 *
 * + Удалить любое содержимое в квадратных скобках. На случай если кто-то пихнул обращение
 * ". прррррриветик  . как, делишки?! :))) {]]}╚ "
 *
 * + Заменить запятую, !, - @ \ / _ ( ) на пробел
 * " прррррриветик . как  делишки?  :    {]]}╚ "
 *
 * + Пропустить из строки только кирилицу, латиницу, числа, знак вопроса, пробелы и точки
 * " прррррриветик . как делишки?         "
 *
 * + Удалить повторяющиеся символы
 * " приветик . как делишки? "
 *
 * + Тримануть
 * "приветик . как делишки?"
 *
 * + Оставить только последее непустое предложение, если их несколько.
 * " как делишки?"
 *
 * + Тримануть
 * "как делишки?"
 *
 * + Заменить синонимы
 * "как дела?"
 *
 * + Заменить фонетически похожие буквы:
 * "как тила?"
 *
 * - Отфильтровать базу по критерию количества слов, лимит +- 1
 *
 * - Для каждого ответа вычислить коэффициент Джаро-Винклера
 *
 * - Подобрать ответ с максимальным коэффициентом
 *
 *
 *
 * user data: {"network":"vk","id":"drfailov","name":"Роман Папуша"}
 * Created by Dr. Failov on 11.04.2014.
 * Updated by Dr. Failov on 15.05.2017.
 */
public class AnswerDatabase extends BotModule {
    private File fileAnswers = null;
    private ArrayList<AnswerMicroElement> answers = new ArrayList<>();
    private JaroWinkler jaroWinkler = null;
    private MessagePreparer messagePreparer = null;
    private AnswerUsageCounter answerUsageCounter = null;
    private BackupsManager backupsManager = null;
    //// TODO: 02.10.2017 рейтинг авторов ответов
    //// TODO: 02.10.2017 Дата последнего ответа от автора


    public AnswerDatabase(ApplicationManager applicationManager) throws Exception {
        super(applicationManager);
        fileAnswers = new File(applicationManager.getHomeFolder(), "answer_database.txt");
        if(!fileAnswers.isFile()){
            log(". Файла базы нет. Загрузка файла answer_database.zip из ресурсов...");
            //get resources
            Resources resources = null;
            if(applicationManager != null && applicationManager.getContext() != null)
                resources = applicationManager.getContext().getResources();
            //copy zip
            File tmpZip = new File(applicationManager.getHomeFolder(), "answer_database.zip");
            log(". Копирование файла answer_database.zip из ресурсов...");
            F.copyFile(R.raw.answer_database, resources, tmpZip);
            //unzip
            log(". Распаковка файла answer_database.zip...");
            try {
                F.unzip(tmpZip, tmpZip.getParentFile());
            }
            catch (Exception e){
                log("! Во время распаковки базы произошла ошибка: " + e.getMessage());
                fileAnswers.delete();
                log("Файл базы удалён после неудачной распаковки: " + !fileAnswers.isFile());
            }
            //check
            if(fileAnswers.isFile()){
                log(". Распаковка базы успешна, удаление временного архива...");
                log(". Удаление временного архива: " + tmpZip.delete());
            }
            else {
                throw new Exception(log("! В результате распаковки файл базы не был получен. Проверьте, чтобы в архиве обязательно был файл answer_database.txt. Если такого файла нет, архив повреждён, нужен другой."));
            }
        }
        answerUsageCounter = new AnswerUsageCounter(applicationManager);
        jaroWinkler = new JaroWinkler();
        messagePreparer = new MessagePreparer(applicationManager);
        backupsManager = new BackupsManager(applicationManager);

        childCommands.add(messagePreparer);
        childCommands.add(answerUsageCounter);
        childCommands.add(backupsManager);
        childCommands.add(new Status(applicationManager));
        childCommands.add(new Database(applicationManager));
        childCommands.add(new AddSpkPatt(applicationManager));
        childCommands.add(new FindSpkPatt(applicationManager));
        childCommands.add(new ShowSpkPatt(applicationManager));
        childCommands.add(new GetSpkPatt(applicationManager));
        childCommands.add(new EditSpkPatt(applicationManager));
        childCommands.add(new RemSpkPatt(applicationManager));
        childCommands.add(new DumpDatabase(applicationManager));
        childCommands.add(new RemSpkKeyw(applicationManager));
        childCommands.add(new ReplSpkKeyw(applicationManager));
        childCommands.add(new FilterMedia(applicationManager));
        childCommands.add(new ReplaceDatabase(applicationManager));
        childCommands.add(new ImportDatabase(applicationManager));
        childCommands.add(new ClearDuplicates(applicationManager));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    load();
                }
                catch (Exception e){
                    e.printStackTrace();
                    log("! Error loading database: " + e.getMessage());
                }
            }
        }, "Loading database").start();

    }
    @Override
    public String getName() {
        return "AnswerDatabase";
    }
    @Override
    public Message processMessage(Message message) {
        if(!hasTreatment(message))
            return super.processMessage(message);
        message = remTreatment(message);

        String noAnswersText = applicationManager.getParameters().get("no_answers_message",
                "К сожалению, я не могу тебе ответить, потому что моя база с ответами пустая.",
                "Ответ при пустой базе",
                "Текст, который получит пользователь, если у бота не будет ответов в базе. " +
                        "Ответов может не быть, например, если пользователь очистит базу.");
        if(answers.size() == 0) {
            Answer toSend = new Answer(noAnswersText);
            message.setAnswer(toSend);
            return message;
        }
        Answer answer = getAnswer(message);
        if(answer == null){
            Answer toSend = new Answer(noAnswersText);
            message.setAnswer(toSend);
            return message;
        }
        message.setAnswer(answer);
        return message;
    }
    public AnswerElement addAnswer(AnswerElement answerElement) throws Exception{
        ArrayList<AnswerElement> inArr = new ArrayList<>();
        inArr.add(answerElement);
        ArrayList<AnswerElement> outArr = addAnswer(inArr);
        if(outArr.size() == 0)
            throw new Exception("Ответ не был добавлен в базу. " +
                    "Вероятно, он дублирует один из ответов которые там уже есть.");
        return outArr.get(0);
    }
    public boolean removeAnswer(AnswerElement answerElement) throws Exception{
        ArrayList<Long> toRemove = new ArrayList<>();
        toRemove.add(answerElement.getId());
        return (removeAnswer(toRemove) > 0);
    }
    public int removeAnswer(ArrayList<Long> answersToRemoveId) throws Exception{
        if(answersToRemoveId.isEmpty())
            throw new Exception("Не получены ID ответов которые нужно удалить.");
        //при вызове этой функции важно, чтобы у ответа сохранился ID
        File fileTmp = new File(applicationManager.getHomeFolder(), "Answer_database.tmp");
        PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
        String line;
        int lineNumber = 0;
        int errors = 0;
        boolean changed = false;
        int removed = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (lineNumber % 1000 == 0)
                log(". Удаление ответа в базе (" + lineNumber + " уже проверено)");
            try {
                JSONObject jsonObject = new JSONObject(line);
                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                if(answersToRemoveId.contains(currentAnswerElement.getId())){
                    //просто нихуя с ним не делать
                    changed = true;
                    removed ++;
                }
                else
                    fileTmpWriter.println(currentAnswerElement.toJson().toString());
            }
            catch (Exception e) {
                e.printStackTrace();
                errors++;
                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
            }
        }
        if (errors != 0)
            log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
        //такого ответа в базе нет
        if(!changed) {
            bufferedReader.close();
            fileTmpWriter.close();
            if(fileTmp.delete())
                throw new Exception(log("! Не могу удалить временный файл! Также: В базе нет ответа с таким ID"));
            throw new Exception("В базе нет ответа с таким ID");
        }
        //заменить ответ в памяти
        log(". Удаление ответа в базе (в оперативке)");
        for (int i = 0; i < answers.size(); i++) {
            AnswerMicroElement answerMicroElement = answers.get(i);
            if(answersToRemoveId.contains(answerMicroElement.getId())){
                answers.remove(answerMicroElement);
            }
        }
        //завешить сессию
        bufferedReader.close();
        fileTmpWriter.close();
        System.gc();
        //Подменить файлы
        backupsManager.backupDatabase("после удаления ответов");
        if(!fileAnswers.delete())
            throw new Exception(log("! Не могу удалить текущий файл с базой " + fileAnswers.getName()));
        if(fileTmp.renameTo(fileAnswers))
            throw new Exception(log("! Не могу перенести новый файл на место старого!" +
                    "Проверь, не запущена ли какая-то длительная процедура, типа фильтрации."));
        return removed;
    }
    public AnswerElement findByAnswerText(String answerText) throws Exception{
        AnswerElement result = null;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
        String line;
        int lineNumber = 0;
        int errors = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (lineNumber % 1000 == 0)
                log(". Поиск ответа в базе (" + lineNumber + " уже проверено) ...");
            try {
                JSONObject jsonObject = new JSONObject(line);
                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                if (currentAnswerElement.getAnswerText().equalsIgnoreCase(answerText)) {
                    result = currentAnswerElement;
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                errors++;
                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
            }
        }
        if (errors != 0)
            log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
        //завешить сессию
        bufferedReader.close();
        System.gc();
        return result;
    }
    public ArrayList<AnswerMicroElement> getAnswers() {
        return answers;
    }

    private Answer getAnswer(Message message){
        ArrayList<AnswerMicroElement> theBest = new ArrayList<>();
        double maxSimilarity = 0;
        AnswerCandidate[] candidates = getAnswers(message);

        for (AnswerCandidate answerCandidate:candidates){
            if(answerCandidate == null)
                continue;
            if(answerCandidate.similarity > maxSimilarity){
                maxSimilarity = answerCandidate.similarity;
                theBest.clear();
                theBest.add(answerCandidate.answer);
            }
            if(answerCandidate.similarity == maxSimilarity){
                theBest.add(answerCandidate.answer);
            }
        }

        if(theBest.isEmpty())
            return null;
        int index = new Random().nextInt(theBest.size());
        AnswerMicroElement answerMicroElement = theBest.get(index);
        answerUsageCounter.increment(answerMicroElement.getId());
        return new Answer(answerMicroElement.getAnswerText(), answerMicroElement.getAnswerAttachments());
    }
    private AnswerCandidate[] getAnswers(Message message){
        AnswerCandidate[] candidates = new AnswerCandidate[20];
        String text = applicationManager.getBrain().remTreatment(message).getText();
        String s1 = messagePreparer.prepare(text);
        int photo1 = message.getPhotosCount();
        int video1 = message.getVideoCount();
        int doc1 = message.getDocumentsCount();
        int music1 = message.getAudioCount();
        int stickers1 = message.getStickersCount();
        int records1 = message.getRecordsCount();
        double maxSimilarity = 0;
        ArrayList<AnswerMicroElement> maxAnswers = new ArrayList<>();
        for (int a=0; a< answers.size(); a++){
            AnswerMicroElement answer = answers.get(a);
            String s2 = answer.getQuestionTextPrepared();
            int photo2 = answer.getQuestionPhotos();
            int music2 = answer.getQuestionMusic();
            int doc2 = answer.getQuestionDocuments();
            int video2 = answer.getQuestionVideos();
            int records2 = answer.getQuestionRecords();
            int stickers2 = answer.getQuestionStickers();

            double similarity = jaroWinkler.similarity(s1, s2); //0...1
            //Если у ответов разный набор вложений, это уменьшит степень их сходства
            similarity -= Math.abs(photo2 - photo1)*0.05;
            similarity -= Math.abs(video2 - video1)*0.05;
            similarity -= Math.abs(doc1 - doc2)*0.05;
            similarity -= Math.abs(music1 - music2)*0.05;
            similarity -= Math.abs(stickers1 - stickers2)*0.05;
            similarity -= Math.abs(records1 - records2)*0.05;

            //На этом месте мы имеем валидную похожесть и можно начинать принимать решения
            if(similarity == maxSimilarity) {
                //сделать сдвиг
                for (int i = candidates.length-1; i > 0; i--)
                    candidates[i] = candidates[i-1];
                candidates[0] = new AnswerCandidate(answer, similarity);
            }
            if(similarity > maxSimilarity){
                for (int i = candidates.length-1; i > 0; i--)
                    candidates[i] = candidates[i-1];
                candidates[0] = new AnswerCandidate(answer, similarity);
                maxSimilarity = similarity;
            }
        }
        //На этом месте у нас уже есть набор макисмально похожих ответов.
        // Тут уже можно принимать решения о том, чтобы, например, добавить
        // вопрос в список неизвестных
        double minUnknownSimilarity = applicationManager.getParameters().get("minUnknownSimilarity",
                0.3d,
                "Минимальная знакомость неизвестных",
                "Минимальное значение похожести сообщения, при котором оно будет внесено " +
                        "в список неизвестных. Всё что будет менее похожее - в список " +
                        "неизвестных не попадает.\n" +
                        "Чем меньше значение похожести сообщения, тем более оно бессмысленно.\n" +
                        "Список неизвестных - это место, откуда берутся ответы для функции обучения.\n" +
                        "Это ограничение нужно для того, чтобы в список неизвестных не " +
                        "попадала совмем бессмысленная белеберда.");
        double maxUnknownSimilarity = applicationManager.getParameters().get("maxUnknownSimilarity",
                0.8d,
                "Максимальная знакомость неизвестных",
                "Максимальное значение похожести сообщения, при котором оно будет внесено " +
                        "в список неизвестных. Всё что будет более похожее - в список " +
                        "неизсестных не попадает\n" +
                        "Чем больше значение похожести сообщения, более точный ответ на него уже есть в базе.\n" +
                        "Список неизвестных - это место, откуда берутся ответы для функции обучения.\n" +
                        "Это ограничение нужно для того, чтобы в список неизвестных не " +
                        "попадали вопросы, ответы на котооые уже есть в базе.");
        if(maxSimilarity > minUnknownSimilarity && maxSimilarity < maxUnknownSimilarity){
            applicationManager.getBrain().getUnknownMessages().add(new UnknownMessage(message.getText(), message.getAttachments(), new Date(), message.getAuthor(), (float)maxSimilarity));
        }
        return candidates;
    }
    private void load() {
        //функция вынесена отдельно для того, чтобы вызывать её в случае изменений базы
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
            MessagePreparer preparer = new MessagePreparer(applicationManager);
            String line;
            int lineNumber = 0;
            int errors = 0;
            long lastId = -1;
            answers.clear();
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber ++;
                if(lineNumber%1000 == 0)
                    log(". База загружается... (" + lineNumber + " загружено)");
                try {
                    JSONObject jsonObject = new JSONObject(line);
                    AnswerMicroElement answerMicroElement = new AnswerMicroElement(jsonObject, preparer);
                    if(answerMicroElement.getId() > lastId) { //OK
                        if(answerMicroElement.isValidated()) {
                            //непроверенные ответы не принимают участия в формировании ответов
                            answers.add(answerMicroElement);
                            lastId = answerMicroElement.getId();
                        }
                    }
                    else {//FAIL
                        messageBox("При загрузке базы было обнаружено повреждение. " +
                                "Сейчас запустится фильтрация базы для восстановления структуры." +
                                " Это займёт некоторое время.");
                        bufferedReader.close();
                        filterDatabase();
                        return;
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                    errors ++;
                    log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                }
            }
            bufferedReader.close();
            log(". Загружено " + answers.size() + " щаблонов ответа.");
            if(errors != 0)
                log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка загрузки модуля работы с базой ответов: " + e.getMessage());
        }
    }
    private void filterDatabase(){
        answers.clear();
        try{
            ArrayList<String> loadedQuestions = new ArrayList<>();
            ArrayList<String> repeatedQuestions = new ArrayList<>();
            ArrayList<AnswerElement> repeatedAnswers = new ArrayList<>();
            //--- Этап 1: Поиск дубликатов
            {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
                String line;
                int lineNumber = 0;
                int errors = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber % 1000 == 0)
                        log(". Фильтрация базы, этап 1 - поиск дубликатов вопросов... (" + lineNumber + " пройдено)");
                    try {
                        JSONObject jsonObject = new JSONObject(line);
                        if (jsonObject.has("questionText")) {
                            String text = jsonObject.getString("questionText");
                            if (loadedQuestions.contains(text)) {
                                if (!repeatedQuestions.contains(text))
                                    repeatedQuestions.add(text);
                            } else {
                                loadedQuestions.add(text);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        errors++;
                        log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                    }
                }
                bufferedReader.close();
                loadedQuestions.clear();
                System.gc();
                log(". Загружено " + answers.size() + " щаблонов ответа.");
                if (errors != 0)
                    log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
                log(". Количество шаблонов с повторяющимися вопросами: " + repeatedQuestions.size() + ".");
            }

            //--- этап 2, фильтрация дубликатов
            //После этого этапа в файле answer_database.tmp будут ответы без дубликатов
            {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
                PrintWriter fileWriter = new PrintWriter(new File(applicationManager.getHomeFolder(), "Answer_database.tmp"));
                String line;
                int lineNumber = 0;
                int errors = 0;
                int uniqueAnswers = 0;
                //Сначала переносим в новый файл те ответы у которых нет аналогов ответв
                //А те что есть, загружаем в память
                while ((line = bufferedReader.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber % 1000 == 0)
                        log(". Фильтрация базы, этап 2 - очистка дубликатов вопросов... (" + lineNumber + " пройдено, "+uniqueAnswers+" уникальных)");
                    try {
                        JSONObject jsonObject = new JSONObject(line);
                        AnswerElement answerElement = new AnswerElement(jsonObject);
                        String question = answerElement.getQuestionText();
                        if(repeatedQuestions.contains(question)){
                            repeatedAnswers.add(answerElement);
                        }
                        else {
                            fileWriter.println(answerElement.toJson().toString());
                            uniqueAnswers ++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        errors++;
                        log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                    }
                }
                bufferedReader.close();
                repeatedQuestions.clear();
                System.gc();
                if (errors != 0)
                    log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
                log(". Загружено " + answers.size() + " шаблонов ответа.");
                //Теперь чистим дубликаты среди тех ответов, которые имеют аналоги
                //На этом месте у нас в массиве repeatedAnswers есть ответы с дубликатами
                int beforeDeduplication = repeatedAnswers.size();
                log(". Очистка " + beforeDeduplication + " ответов от дубликатов...");
                for(int i=0; i<repeatedAnswers.size()-1; i++){
                    for(int j=i+1; j<repeatedAnswers.size(); j++){
                        AnswerElement answer1 = repeatedAnswers.get(i);
                        AnswerElement answer2 = repeatedAnswers.get(j);
                        if(answer1.equalAnswer(answer2))
                            repeatedAnswers.remove(answer2);
                    }
                }
                int afterDeduplication = repeatedAnswers.size();
                log(". Удалено" + (beforeDeduplication - afterDeduplication) + " дубликатов ответов.");
                //Теперь очищенные от дубликатов ответы дописываем в файл
                for(int i=0; i<repeatedAnswers.size(); i++){
                    fileWriter.println(repeatedAnswers.get(i).toJson().toString());
                }
                log(". " + afterDeduplication + " ответов записано в файл.");
                fileWriter.close();
                int totalAfterDeduplication = uniqueAnswers + afterDeduplication;
                log(". После очистки дубликатов осталось " + totalAfterDeduplication + " ответов.");
                log(".");
            }


            //--- Этап 3, сортировка ID
            //Вся база должна быть в неспадающей последовательности.
            // Если мы обнаруживаем наружение этой последовательности,
            // то все ответы ПОСЛЕ этого получают новые ID, которые не
            // нарушат последовательность
            {
                boolean fixing = false;
                long last = -1;
                BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(applicationManager.getHomeFolder(), "Answer_database.tmp")));
                PrintWriter fileWriter = new PrintWriter(new File(applicationManager.getHomeFolder(), "Answer_database.tmp1"));
                String line;
                int lineNumber = 0;
                int errors = 0;
                int fixedAnswers = 0;

                while ((line = bufferedReader.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber % 1000 == 0)
                        log(". Фильтрация базы, этап 3 - исправление ID ответов... (" + lineNumber + " пройдено, "+fixedAnswers+" исправлено)");
                    try {
                        JSONObject jsonObject = new JSONObject(line);
                        AnswerElement answerElement = new AnswerElement(jsonObject);
                        if(fixing){ //Если мы в режиме исправления
                            answerElement.setId(last + 1);
                            fixedAnswers ++;
                            fileWriter.println(answerElement.toJson().toString());
                            last ++;
                        }
                        else {  //Если мы в режиме чтения
                            long id = answerElement.getId();
                            if(id > last){//Если всё ок
                                fileWriter.println(answerElement.toJson().toString());
                                last = id;
                            }
                            else { //Если последовательность нарушена
                                answerElement.setId(last + 1);
                                fixedAnswers ++;
                                last ++;
                                fixing = true;
                                fileWriter.println(answerElement.toJson().toString());
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        errors++;
                        log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                    }
                }
                bufferedReader.close();
                fileWriter.close();
                System.gc();
                if (errors != 0)
                    log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
                log(". Во время исправления было исправлено " + fixedAnswers+ " ID ответов.");
            }
            //--- ЭТАП 4: Заменить файлы
            {
                backupsManager.backupDatabase("после фильтрации");
                File trashFile = new File(applicationManager.getHomeFolder(), "Answer_database.tmp");
                if(!trashFile.delete())
                    log("! Не могу удалить временный файл " + trashFile.getName());
                File resultFile = new File(applicationManager.getHomeFolder(), "Answer_database.tmp1");
                if(!resultFile.renameTo(fileAnswers))
                    log("! Не могу пененести на место базы файл " + resultFile.getName());
            }
            //--- ЭТАП 5: загрузить базу повторно
            log(". Фильтрация завершена! Повторная загрузка базы...");
            load();
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка фильтрации базы ответов: " + e.getMessage());
        }
    }
    private ArrayList<AnswerElement> addAnswer(ArrayList<AnswerElement> answerElements) throws Exception{
        if(answerElements == null)
            throw new Exception("Ответы для добавления не был получен");
        if(answerElements.size() == 0)
            throw new Exception("Ответ для добавления не был получен");
        for(AnswerElement answerElement:answerElements) {
            if (answerElement == null)
                throw new Exception("Ответ для добавления не был получен");
            if (answerElement.getAnswerText().equals("") && answerElement.getAnswerAttachments().isEmpty())
                throw new Exception("Ответ для добавления не содержит в поле ответа ни вложений ни текста");
            if (answerElement.getQuestionText().equals("") && answerElement.getQuestionAttachments().equals(""))
                throw new Exception("Ответ для добавления не содержит в поле вопроса ни текста ни вложений");
            if (answerElement.getAnswerText().length() > 4000)
                throw new Exception("Ответ для добавления содержит слишком длинный ответ");
            if (answerElement.getQuestionText().length() > 4000)
                throw new Exception("Ответ для добавления содержит слишком длинный вопрос");
            if (answerElement.getAnswerAttachments().size() > 10)
                throw new Exception("Ответ для добавления содержит слишком много вложений");
        }

        File fileTmp = new File(applicationManager.getHomeFolder(), "Answer_database.tmp");
        PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
        String line;
        int lineNumber = 0;
        int errors = 0;
        long maxId = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (lineNumber % 1000 == 0)
                log(". Добавление ответа в базу (" + lineNumber + " уже проверено)");
            try {
                JSONObject jsonObject = new JSONObject(line);
                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                if(hasSame(answerElements, currentAnswerElement))
                    answerElements = remSame(answerElements, currentAnswerElement);
                fileTmpWriter.println(currentAnswerElement.toJson().toString());
                maxId = Math.max(currentAnswerElement.getId(), maxId);
            }
            catch (Exception e) {
                e.printStackTrace();
                errors++;
                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
            }
        }
        //если во время первого прохода по базе данных все варианты для добавления были удалены
        if (answerElements.size() == 0) {
            //BREAK
            bufferedReader.close();
            fileTmpWriter.close();
            if (!fileTmp.delete())
                log("! Не могу удалить за собой временный файл " + fileTmp.getName());
            System.gc();
            throw new Exception("Такой ответ уже есть в базе ответов.");
        }

        //такого ответа в базе ещё нет
        for(AnswerElement answerElement:answerElements) {
            answerElement.setId(maxId + 1);
            maxId ++;
            fileTmpWriter.println(answerElement.toJson().toString());
            answers.add(new AnswerMicroElement(answerElement.toJson(), new MessagePreparer(applicationManager)));
        }
        //завешить сессию
        bufferedReader.close();
        fileTmpWriter.close();
        System.gc();
        if (errors != 0)
            log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
        //Подменить файлы
        backupsManager.backupDatabase("после добавления");
        if(!fileAnswers.delete())
            throw new Exception(log("! Не могу удалить текущий файл с базой " + fileAnswers.getName()));
        if(fileTmp.renameTo(fileAnswers))
            throw new Exception(log("! Не могу перенести новый файл на место старого! " +
                    "Проверь, не запущена ли какая-то длительная процедура, типа фильтрации."));
        return answerElements;
    }
    private boolean hasSame(ArrayList<AnswerElement> answerElements, AnswerElement answerElement){
        for (AnswerElement answerElement1:answerElements)
            if(answerElement1.equalAnswer(answerElement))
                return true;
        return false;
    }
    private ArrayList<AnswerElement> remSame(ArrayList<AnswerElement> answerElements, AnswerElement answerElement){
        for (int i=0; i<answerElements.size(); i++)
            if(answerElements.get(i).equalAnswer(answerElement))
                answerElements.remove(i--);
        return answerElements;
    }
    private void editAnswer(long id, Answer newAnswer, User autor) throws Exception{
        if(newAnswer == null)
            throw new Exception("Ответ для редактирования не был получен!");
        if(id == 0)
            throw new Exception("Ответ для редактирования не содержит корректного ID");
        //при вызове этой функции важно, чтобы у ответа сохранился ID
        File fileTmp = new File(applicationManager.getHomeFolder(), "Answer_database.tmp");
        PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
        String line;
        int lineNumber = 0;
        int errors = 0;
        AnswerElement changed = null;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (lineNumber % 1000 == 0)
                log(". Изменение ответа в базе (" + lineNumber + " уже проверено)");
            try {
                JSONObject jsonObject = new JSONObject(line);
                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                if(currentAnswerElement.getId() == id){
                    currentAnswerElement.setAnswerText(newAnswer.text, autor);
                    currentAnswerElement.setAnswerAttachments(newAnswer.attachments);
                    changed = currentAnswerElement;
                }
                fileTmpWriter.println(currentAnswerElement.toJson().toString());
            }
            catch (Exception e) {
                e.printStackTrace();
                errors++;
                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
            }
        }
        if (errors != 0)
            log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
        //такого ответа в базе нет
        if(changed == null) {
            bufferedReader.close();
            fileTmpWriter.close();
            if(fileTmp.delete())
                throw new Exception(log("! Не могу удалить временный файл! Также: В базе нет ответа с таким ID"));
            throw new Exception("В базе нет ответа с таким ID");
        }
        //заменить ответ в памяти
        log(". Изменение ответа в базе (в оперативке)");
        for (int i = 0; i < answers.size(); i++) {
            AnswerMicroElement answerMicroElement = answers.get(i);
            if(answerMicroElement.getId() == id){
                answers.remove(answerMicroElement);
                answers.add(i, new AnswerMicroElement(
                        changed.toJson(),
                        new MessagePreparer(applicationManager)));
                break;
            }
        }
        //завешить сессию
        bufferedReader.close();
        fileTmpWriter.close();
        System.gc();
        //Подменить файлы
        backupsManager.backupDatabase("после редактирования ответа");
        if(!fileAnswers.delete())
            throw new Exception(log("! Не могу удалить текущий файл с базой " + fileAnswers.getName()));
        if(fileTmp.renameTo(fileAnswers))
            throw new Exception(log("! Не могу перенести новый файл на место старого!" +
                    "Проверь, не запущена ли какая-то длительная процедура, типа фильтрации."));
    }
    private void importAnswers(File toImport) throws Exception{
        if(toImport == null)
            throw new Exception("Не получен файл для импорта");
        if(!toImport.exists())
            throw new Exception("Файл для импорта не сущеструет.");
        File fileTmp = new File(applicationManager.getHomeFolder(), "Answer_database.tmp");
        PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
        BufferedReader currentReader = new BufferedReader(new FileReader(fileAnswers));
        BufferedReader newReader = new BufferedReader(new FileReader(fileAnswers));
        String line;
        int lineNumber = 0;
        int errors = 0;
        long maxId = 0;

        //Перенос старой базы
        while ((line = currentReader.readLine()) != null) {
            lineNumber++;
            if (lineNumber % 1000 == 0)
                log(". Импорт базы данных... (" + lineNumber + " старых ответов уже перенесено из старой базы)");
            try {
                JSONObject jsonObject = new JSONObject(line);
                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                if(currentAnswerElement.getId() > maxId)
                    maxId = currentAnswerElement.getId();
                fileTmpWriter.println(currentAnswerElement.toJson().toString());
            }
            catch (Exception e) {
                e.printStackTrace();
                errors++;
                log("! Ошибка разбора строки " + lineNumber + " как ответа из старой базы.\n" + e.getMessage());
            }
        }
        if (errors != 0)
            log("! При загрузке старой базы ответов возникло ошибок: " + errors + ".");


        //перенос новой базы
        while ((line = newReader.readLine()) != null) {
            lineNumber++;
            if (lineNumber % 1000 == 0)
                log(". Импорт базы данных... (" + lineNumber + " новых ответов уже перенесено из новой базы)");
            try {
                JSONObject jsonObject = new JSONObject(line);
                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                maxId ++;
                currentAnswerElement.setId(maxId);
                fileTmpWriter.println(currentAnswerElement.toJson().toString());
            }
            catch (Exception e) {
                e.printStackTrace();
                errors++;
                log("! Ошибка разбора строки " + lineNumber + " как ответа из новой базы.\n" + e.getMessage());
            }
        }
        if (errors != 0)
            log("! При загрузке новой базы ответов возникло ошибок: " + errors + ".");


        //завешить сессию
        currentReader.close();
        newReader.close();
        fileTmpWriter.close();
        System.gc();
        //Подменить файлы
        backupsManager.backupDatabase("перед импортом базы данных");
        if(!fileAnswers.delete())
            throw new Exception(log("! Не могу удалить текущий файл с базой " + fileAnswers.getName()));
        if(fileTmp.renameTo(fileAnswers))
            throw new Exception(log("! Не могу перенести новый файл на место старого!" +
                    "Проверь, не запущена ли какая-то длительная процедура, типа фильтрации."));
        load();
        //операция выполнена
    }
    private ArrayList<AnswerElement> getAnswersById(ArrayList<Long> ids){
        ArrayList<AnswerElement> result = new ArrayList<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
            String line;
            int lineNumber = 0;
            int errors = 0;

            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                if (lineNumber % 1000 == 0)
                    log(". Поиск ответов в базе (" + lineNumber + " уже проверено)");
                try {
                    JSONObject jsonObject = new JSONObject(line);
                    AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                    if (ids.contains(currentAnswerElement.getId()))
                        result.add(currentAnswerElement);
                } catch (Exception e) {
                    e.printStackTrace();
                    errors++;
                    log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                }
            }
            if (errors != 0)
                log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
            //завешить сессию
            bufferedReader.close();
            System.gc();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
    private AnswerElement getAnswerById(long id){
        ArrayList<Long> list = new ArrayList<>();
        list.add(id);
        ArrayList<AnswerElement> answerElements = getAnswersById(list);
        if(answerElements.isEmpty())
            return null;
        return answerElements.get(0);
    }
    private ArrayList<AnswerElement> findAnswers(String text, int offset) throws Exception{
        ArrayList<AnswerElement> result = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
        String line;
        int lineNumber = 0;
        int errors = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (lineNumber % 1000 == 0)
                log(". Поиск ответов в базе (" + lineNumber + " уже проверено)");
            try {
                JSONObject jsonObject = new JSONObject(line);
                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                if (currentAnswerElement.getAnswerText().toLowerCase().contains(text.toLowerCase())
                        ||currentAnswerElement.getQuestionText().toLowerCase().contains(text.toLowerCase())) {
                    if (offset-- < 0) {//пропустить первые offset ответов
                        result.add(currentAnswerElement);
                        if(result.size() >= 30)
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                errors++;
                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
            }
        }
        if (errors != 0)
            log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
        //завешить сессию
        bufferedReader.close();
        System.gc();
        return result;
    }
    private int removeAllAnswersContains(String text) throws Exception{
        if(text.equals(""))
            throw new Exception("Текст для удаления не был получен");
        //при вызове этой функции важно, чтобы у ответа сохранился ID
        File fileTmp = new File(applicationManager.getHomeFolder(), "Answer_database.tmp");
        PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
        String line;
        int lineNumber = 0;
        int errors = 0;
        int removed = 0;
        ArrayList<AnswerMicroElement> toRemoveFromMemory = new ArrayList<>();

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (lineNumber % 1000 == 0)
                log(". Удаление ответов в базе (" + lineNumber + " уже проверено)");
            try {
                text = text.toLowerCase();
                JSONObject jsonObject = new JSONObject(line);
                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                if(currentAnswerElement.getAnswerText().toLowerCase().contains(text)){
                    //просто нихуя с ним не делать
                    removed ++;
                    //добавить ответ в памяти к списку того, что надо из памяти удалить
                    for (AnswerMicroElement answerMicroElement:answers) {
                        if (answerMicroElement.getId() == currentAnswerElement.getId()) {
                            toRemoveFromMemory.add(answerMicroElement);
                            break;
                        }
                    }
                }
                else
                    fileTmpWriter.println(currentAnswerElement.toJson().toString());
            }
            catch (Exception e) {
                e.printStackTrace();
                errors++;
                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
            }
        }
        if (errors != 0)
            log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
        //заменить ответ в памяти
        log(". Удаление ответа в базе (в оперативке)");
        answers.removeAll(toRemoveFromMemory);
        //завешить сессию
        bufferedReader.close();
        fileTmpWriter.close();
        System.gc();
        //Подменить файлы
        backupsManager.backupDatabase("после удаления ответов");
        if(!fileAnswers.delete())
            throw new Exception(log("! Не могу удалить текущий файл с базой " + fileAnswers.getName()));
        if(fileTmp.renameTo(fileAnswers))
            throw new Exception(log("! Не могу перенести новый файл на место старого!" +
                    "Проверь, не запущена ли какая-то длительная процедура, типа фильтрации."));
        return removed;
    }
    private int replaceAllAnswersContains(String text, String toReplace, User author) throws Exception{
        if(text.equals(""))
            throw new Exception("Текст, который нужно заменить, не был получен.");
        if(toReplace.equals(""))
            throw new Exception("Текст, который надо подставить на место замены, не был получен");
        //при вызове этой функции важно, чтобы у ответа сохранился ID
        File fileTmp = new File(applicationManager.getHomeFolder(), "Answer_database.tmp");
        PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
        String line;
        int lineNumber = 0;
        int errors = 0;
        int changed = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (lineNumber % 1000 == 0)
                log(". Замена ответов в базе (" + lineNumber + " уже проверено)");
            try {
                text = text.toLowerCase();
                JSONObject jsonObject = new JSONObject(line);
                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                if(currentAnswerElement.getAnswerText().contains(text)){
                    //Заменить
                    currentAnswerElement.setAnswerText(currentAnswerElement.getAnswerText().replace(text, toReplace), author);
                    changed ++;
                    //заменить ответ в памяти
                    for (AnswerMicroElement answerMicroElement:answers) {
                        if (answerMicroElement.getId() == currentAnswerElement.getId()) {
                            answerMicroElement.setAnswerText(answerMicroElement.getAnswerText().replace(text, toReplace));
                            break;
                        }
                    }
                }
                fileTmpWriter.println(currentAnswerElement.toJson().toString());
            }
            catch (Exception e) {
                e.printStackTrace();
                errors++;
                log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
            }
        }
        if (errors != 0)
            log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
        //завешить сессию
        bufferedReader.close();
        fileTmpWriter.close();
        System.gc();
        //Подменить файлы
        backupsManager.backupDatabase("после пакетной замены "+changed+" ответов в базе");
        if(!fileAnswers.delete())
            throw new Exception(log("! Не могу удалить текущий файл с базой " + fileAnswers.getName()));
        if(fileTmp.renameTo(fileAnswers))
            throw new Exception(log("! Не могу перенести новый файл на место старого!" +
                    "Проверь, не запущена ли какая-то длительная процедура, типа фильтрации."));
        return changed;
    }

    private class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("status"))
                return "Размер базы ответов: " + answers.size() + "\n" +
                        "Файл базы ответов: " + fileAnswers.getPath() + "\n";
            return "";
        }
    }
    private class Database extends CommandModule{
        public Database(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();

            result.add(new CommandDesc("Вывести количество ответов в базе",
                    "\"Database\" означает \"База данных\".\n" +
                            "Чем больше у твоего бота в базе ответов, тем умнее твой бот! " +
                            "Эта команда поможет тебе узнать, насколько умный твой бот.",
                    "botcmd database"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("database"))
                return "В базе ответов твоего бота: " + answers.size() + " ответов.\n";
            return "";
        }
    }
    private class ShowSpkPatt extends CommandModule{
        public ShowSpkPatt(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Показать ответы из базы",
                    //// TODO: 25.07.2017 прописать такую же "справочку" ко многим другим командам
                    //// TODO: 25.07.2017 проверить, реально ли Speaking правильно здесь
                    "\"ShowSpkPatt\" - это сокращение от \"Show Speaking Patterns\" - \"Показать разговорные шаблоны\".\n" +
                            "Эта команда позволяет тебе просмотреть ответы в базе данных. " +
                            "Чтобы определить какие ответы ты хочешь увидеть, можно вписать номера через запятую, " +
                            "можно указать промежуток через тире.\n" +
                            "Одной командой можно просмотреть максимум 50 ответов.\n" +
                            "Если указан всего один ответ, команда выдаст более полную информацию про него.",
                    "botcmd ShowSpkPatt <список ID ответов>"));
            result.add(new CommandDesc("Показать ответы из базы",
                    "Это сокращённый вариант написания команды \"botcmd ShowSpkPatt\". Они работают " +
                            "абсолютно одинаково, только эту писать проще.",
                    "botcmd ssp <список ID ответов>"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String keyword = commandParser.getWord();
            if(keyword.toLowerCase().equals("showspkpatt") || keyword.toLowerCase().equals("ssp")){
                try {
                    String invetral = commandParser.getText().trim();
                    ArrayList<Integer> toShow = F.parseRange(invetral, answers.size() * 3, 50);
                    if(toShow.isEmpty())
                        throw new Exception("Не выбрано ни одного элемента.");
                    if(toShow.size() == 1){
                        int index = toShow.get(0);
                        AnswerElement answerElement = getAnswerById(index);
                        if(answerElement == null)
                            return "Элемента с ID " + index + " нету в базе.";
                        return "Элемент " + index + ": \n" + answerElement.toString();
                    }
                    ArrayList<AnswerElement> answerElements = getAnswersById(F.integerArrayToLongArray(toShow));
                    if(answerElements.isEmpty())
                        throw new Exception("Таких ID нет в базе.");
                    String result = "Отобрано " + answerElements.size() + " ответов:\n";
                    for(AnswerElement answerElement:answerElements)
                        result += answerElement.toShortString() + "\n";
                    return result;
                }
                catch (Exception e){
                    return "Ошибка: " + e.getMessage() + "\n" +
                            "Формат команды:\n"+
                            F.commandDescToText(getHelp().get(0));
                }
            }
            return "";
        }
    }
    private class AddSpkPatt extends CommandModule{
        public AddSpkPatt(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Добавить ответ на сообщение в базу",
                    "Если ты хочешь задать, чтобы на вопрос бот отвечал как-то по другому, " +
                            "ты можешь задать это этой командой.\n" +
                            "Некоторые ключевые слова будут заменяться на разную информацию при " +
                            "подборе ответа. Например:" +
                            "\"Привет %USER NAME%!\" заменится на \"Привет, Виктор!\", если " +
                            "бот будет отвечать Виктору.\n" +
                            "Вот полный список ключевых слов, которые заменятся при ответе:\n" +
                            "%USER NAME% (без пробела) - имя собеседника\n" +
                            "%USER SURNAME% (без пробела) - фамилия собеседника\n" +
                            "%USER STATUS% (без пробела) - текст статуса собеседника\n" +
                            "%USER DOMAIN% (без пробела) - адрес страницы юзера (ScreenName)\n" +
                            "%USER PHOTO% (без пробела) - вложит в ответ аватарку собеседника\n" +
                            "%DA TE% (без пробела) - текущая дата\n" +
                            "%TI ME% (без пробела) - текущее время\n" +
                            "\"\\n\" - переход к новой строке", //// TODO: 13.07.2017 Но это не точно. Проверить.
                    "botcmd AddSpkPatt <вопрос>*<ответ>"));
            //Все эти замены происходят в файле BotBrain
            result.add(new CommandDesc("Добавить ответ на сообщение в базу",
                    "Это сокращённый вариант написания команды \"botcmd AddSpkPatt\". Они работают " +
                            "абсолютно одинаково, только эту писать проще.",
                    "botcmd asp <вопрос>*<ответ>"));
            // TODO: 13.07.2017 В центральном распределительном месте всех комаанд нужно
            // сделать пометку о том, что любая команда может использовать bcd вместо botcmd
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String keyword = commandParser.getWord();
            if(keyword.toLowerCase().equals("addspkpatt") || keyword.toLowerCase().equals("asp")){
                String lastText = commandParser.getText();
                if (lastText.equals(""))
                    return "Не могу получить ответы. Чтобы эта команда сработала, " +
                            "надо писать так: addspkpatt <вопрос>*<ответ>.\n" +
                            "Например: addspkpatt Как дела?*Нормально!\n" +
                            "Информация из справки: \n"+
                            F.commandDescToText(getHelp().get(0));
                String[] messages = lastText.split("\\*");
                if (messages.length < 2)
                    return "Не могу получить ответ на вопрос. Чтобы эта команда сработала, " +
                            "надо писать так: addspkpatt <вопрос>*<ответ>.\n" +
                            "Например: addspkpatt Как дела?*Нормально!\n" +
                            "Информация из справки: \n"+
                            F.commandDescToText(getHelp().get(0));
                try{
                    AnswerElement answerElement = new AnswerElement(message.getAuthor(), messages[0], messages[1]);
                    answerElement.setAnswerAttachments(message.getAttachments());
                    answerElement = addAnswer(answerElement);
                    return "Ответ добавлен: " + answerElement.toString() + "\n";
                }
                catch (Exception e){
                    return e.getMessage() + "\n";
                }
            }
            return "";
        }
    }
    private class GetSpkPatt extends CommandModule{
        GetSpkPatt(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Вывести варианты ответа на вопрос",
                    "Покажет какие в базе бота есть шаблоны, которые подходят для " +
                            "ответа на вопрос.\n" +
                            "Эта команда будет полезна для отладки, или для изучения базы.\n" +
                            "Когда бот подбирает ответ для пользователя, он находит самые подходящие ответы " +
                            "(это те, у который самый высокий similarity) и среди них случайным образом выбирает ответ.",
                    "botcmd GetSpkPatt <вопрос>"));
            result.add(new CommandDesc("Вывести варианты ответа на вопрос",
                    "Это сокращённый вариант написания команды \"botcmd GetSpkPatt\". Они работают " +
                            "абсолютно одинаково, только эту писать проще.",
                    "botcmd gsp <вопрос>"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String keyword = commandParser.getWord();
            if(keyword.toLowerCase().equals("getspkpatt") || keyword.toLowerCase().equals("gsp")){
                try {
                    message.setText(message.getText()
                            .replace("bcd", "").replace("botcmd", "")
                            .replace("getspkpatt", "").replace("gsp", "").trim());
                    log("Getting answer for " + message.getText());
                    AnswerCandidate[] answerCandidates = getAnswers(message);
                    String result = "Варианты ответа на сообщение \""+message.getText()+"\":";
                    for (int i = 0; i < answerCandidates.length; i++) {
                        if(answerCandidates[i] == null)
                            break;
                        result += answerCandidates[i].toString() + "\n";
                    }
                    return result;
                }
                catch (Exception e){
                    e.printStackTrace();
                    return "Ошибка подбора вариантов ответа: " + e.getMessage()
                            + "\nПопробуй проверить, правильно ли введена команда, и в норме ли база.";
                }
            }
            return "";
        }
    }
    private class RemSpkPatt extends CommandModule{
        RemSpkPatt(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить шаблон ответа из базы",
                    "Если какой-то ответ бота тебе не нравится, ты можешь его просто удалить из базы ответов.\n" +
                            "Сюда надо писать ID ответа. Чтобы получить ID нужного тебе ответа, " +
                            "можно воспользоваться командой botcmd ShowSpkPatt и просматривать базу в поисках нужного ответа, " +
                            "либо можно воспользоваться командой botcmd GetSpkPatt и найти шаблон как ответ на какую-то фразу.\n",
                    "botcmd RemSpkPatt <ID ответов, которые надо удалить>"));
            result.add(new CommandDesc("Удалить шаблон ответа из базы",
                    "Это сокращённый вариант написания команды \"botcmd RemSpkPatt\". Они работают " +
                            "абсолютно одинаково, только эту писать проще.",
                    "botcmd rsp <ID ответов, которые надо удалить>"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String word = commandParser.getWord();
            if(word.toLowerCase().equals("remspkpatt") || word.toLowerCase().equals("rsp")){
                String lastText = commandParser.getText();
                if (lastText.equals(""))
                    return "Укажите ID ответов, которые надо удалить.\n"
                            + "\n Попробуй проверить, правильно ли введена команда." +
                            "Формат команды:\n" +
                            F.commandDescToText(getHelp().get(0));
                try {
                    ArrayList<Integer> toDeleteIDs = F.parseRange(lastText, answers.size() * 3, answers.size() * 2);
                    int removed = removeAnswer(F.integerArrayToLongArray(toDeleteIDs));
                    return "Удалено ответов: " + removed;
                }
                catch (Exception e){
                    return "Не могу удалить ответы: " + e.getMessage()
                            + "\n Справка по команде: \n" +
                            F.commandDescToText(getHelp().get(0));
                }
            }
            return "";
        }
    }
    private class EditSpkPatt extends CommandModule{
        EditSpkPatt(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Редактировать текст ответа в базе",
                    "Если какой-то шаблон бота тебе не нравится, ты можешь изменить текст ответа в нём\n" +
                            "Сюда надо писать новый текст ответа (можно вложения тоже) и ID шаблона. Чтобы получить ID нужного тебе шаблона, " +
                            "можно воспользоваться командой botcmd ShowSpkPatt и просматривать базу в поисках нужного ответа, " +
                            "либо можно воспользоваться командой botcmd GetSpkPatt и найти шаблон как ответ на какую-то фразу.\n",
                    "botcmd EditSpkPatt <ID шаблона> <Новый текст ответа>"));
            result.add(new CommandDesc("Редактировать текст ответа в базе",
                    "Это сокращённый вариант написания команды \"botcmd EditSpkPatt\". Они работают " +
                            "абсолютно одинаково, только эту писать проще.",
                    "botcmd esp <ID шаблона> <Новый текст ответа>"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String word = commandParser.getWord();
            if(word.toLowerCase().equals("editspkpatt") || word.toLowerCase().equals("esp")){
                long id = commandParser.getLong();
                String lastText = commandParser.getText();
                ArrayList<Attachment> attachments = message.getAttachments();
                if (lastText.equals(""))
                    return "Укажи текст ответа, на который надо заменить." +
                            "Формат команды:\n" +
                            F.commandDescToText(getHelp().get(0));
                try {
                    editAnswer(id, new Answer(lastText, attachments), message.getAuthor());
                }
                catch (Exception e){
                    return e.getMessage()
                            + "\n Справка по команде: \n" +
                            F.commandDescToText(getHelp().get(0));
                }
            }
            return "";
        }
    }
    private class FindSpkPatt extends CommandModule{
        public FindSpkPatt(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Найти ответы в базе",
                    "Найти ответы в базе, которые содержат ключевую фразу в вопросе, или в ответе. " +
                            "Поиск работает без учёта регистра.\n" +
                            "За раз можно получить не более 30 результатов.\n" +
                            "Сдвиг нужен для того, чтобы отбросить некоторое " +
                            "количество результатов поиска, если их больше 30.\n" +
                            "Стандартное значение сдвига - 0.",
                    "botcmd FindSpkPatt <сдвиг> <текст для поиска>"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("findspkpatt")){
                try{
                    int offset = commandParser.getInt();
                    String text = commandParser.getText();
                    if(offset < 0)
                        message.sendAnswer("Зачем писать отрицательный сдвиг? " +
                                "Это не помешает поиску, но это бесполезно.");
                    if(offset > answers.size())
                        return ("У тебя сдвиг больше количества ответов. " +
                                "Нету результатов. Логично, правда?");
                    if(text.equals(""))
                        return "Текст для поиска не получен. Проверь, правильно ли ты ввёл команду: \n" +
                                F.commandDescToText(getHelp().get(0));
                    ArrayList<AnswerElement> answerElements = findAnswers(text, offset);
                    if(answerElements.isEmpty())
                        return "По запросу \"" + text + "\" со сдвигом " + offset + " ничего не найдено.";
                    String result = "Найдено "+answerElements.size()+" ответов по запросу \"" + text + "\" со сдвигом " + offset + ":\n";
                    for (int i = 0; i < answerElements.size() && i < 30; i++) {
                        result += answerElements.get(i).toShortString() + "\n";
                    }
                    if(answerElements.size() > 30)
                        result += "Показано 30 элементов из " + answerElements.size();
                    return result;
                }catch (Exception e){
                    e.printStackTrace();
                    return "Проблема при выполнении поиска: " + e.getMessage();
                }catch (OutOfMemoryError e){
                    return "Для выполнения поиска недостаточно памяти. " +
                            "Попробуй уточнить параметры поиска, чтобы было меньше результатов.";
                }

            }
            return "";
        }
    }
    private class DumpDatabase extends CommandModule{
        DumpDatabase(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Скачать базу данных как файл",
                    "Загрузить базу данных как файл и отправить тебе.\n" +
                            "Эта команда будет полезна для резервного копирования базы данных, " +
                            "или если ты хочешь поделиться своей базой с кем-то.",
                    "botcmd DumpDataBase"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("dumpdatabase")){
                Answer answer = new Answer("База ответов:", new Attachment(Attachment.TYPE_DOC, fileAnswers));
                message.sendAnswer(answer);
                return "Команда выполнена.";
//                if(!(message.getBotAccount() instanceof VkAccount))
//                    return "Эта команда поддерживается только для VK аккаунта";
//
//                message.sendAnswer("Загружаю документ...");
//                Document document = ((VkAccount)message.getBotAccount()).uploadDocument(fileAnswers);
//                if(document == null)
//                    return "Не удалось выгрузить документ на сервер.\n";
//                else {
//                    Attachment attachment = new Attachment(document);
//                    Answer answer = new Answer("База ответов:", attachment);
//                    message.sendAnswer(answer);
//                    return "Команда выполнена.";
//                }
            }
            return "";
        }
    }
    private class RemSpkKeyw extends CommandModule{
        RemSpkKeyw(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить из базы все ответы, которые содержат фразу",
                    "Из базы удалятся все шаблоны, где в тексте ответа встречается фраза или слово." +
                            " Без учёта регистра.",
                    "botcmd RemSpkKeyw <фраза>"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String word = commandParser.getWord();
            if(word.toLowerCase().equals("remspkkeyw")){
                String text = commandParser.getText();
                if (text.equals(""))
                    return "Укажите текст, ответы с которым надо удалить.\n" +
                            "Формат команды:\n" +
                            F.commandDescToText(getHelp().get(0));
                try {
                    int count = removeAllAnswersContains(text);
                    if(count == 0)
                        return "В базе не найдено ни одного шаблона, в ответе которого бы содержался фрагмент \""+text+"\"";
                    return "Из базы было удалено " + count + " ответов, которые содержали в ответе фрагмент \""+text+"\"";
                }
                catch (Exception e){
                    return "Не могу удалить ответы: " + e.getMessage()
                            + "\n Попробуй проверить, правильно ли введена команда."
                            + "\n Справка по команде: \n" +
                            F.commandDescToText(getHelp().get(0));
                }
            }
            return "";
        }
    }
    private class ReplSpkKeyw extends CommandModule{
        public ReplSpkKeyw(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Массово заменить текст ответов по всей базе",
                    "\"ReplSpkKeyw\" - это сокращение от \"Show speaking patterns\" \"Заменить разговорные шаблоны\".\n" +
                            "Эта команда находит в ответах на вопросы ключевую фразу, и заменяет её на указанный текст.\n" +
                            "Поиск в данном случае работает с учётом регистра. ",
                    "botcmd ReplSpkKeyw <ключевая фраза>*<на что её заменить>"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String keyword = commandParser.getWord();
            if (keyword.toLowerCase().equals("replspkkeyw")){
                try {
                    String lastText = commandParser.getText();
                    if (lastText.equals(""))
                        return "Ошибка замены шаблона: не получены сообщения.\n" +
                                "Проверь формат команды: " + F.commandDescToText(getHelp().get(0));
                    String[] messages = lastText.split("\\*");
                    if (messages.length < 2)
                        return "Ошибка замены шаблона: не получено второе сообщение." +
                                "Ты точно не забыл поставить * между сообщениями?\n" +
                                F.commandDescToText(getHelp().get(0));
                    int replaced = replaceAllAnswersContains(messages[0], messages[1], message.getAuthor());
                    if (replaced == 0)
                        return "В базе не найдено ни одного ответа, который содержал бы \""+messages[0]+"\"";
                    else
                        return "Выполнена замена для " + replaced + " ответов в базе.";
                }
                catch (Exception e){
                    return "Проблема: " + e.getMessage();
                }
            }
            return "";
        }
    }
    private class FilterMedia extends CommandModule{
        private Thread filterThread = null;//when filtering is in progress, make not null
        private long timeStarted = 0; //время когда была начата фильтрация
        private long timePing = 0; //это число показывает момент в который
        // было последнее действие от потока оно
        // позволяет определить завис поток или нет
        private long totalAnswers = 0; //эта пара переменных позволит определить прогресс фильтрации базы данных
        private long processedAnswers = 0;
        private long failedAnswers = 0;
        private long removedAnswers = 0;


        public FilterMedia(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Очистить базу от медиа, которые не доступны",
                    "Поскольку в ответах используются вложения, которые являются ссылками на объекты на страницах пользователей, " +
                            "часто возникает ситуация, когда вложения становятся недоступными из-за того, что пользователь " +
                            "удалил страницу или альбом. " +
                            "Если бот будет использовать ответы с несуществующими вложениями, " +
                            "это будет выглядеть как пустое сообщение.\n" +
                            "Чтобы не допускать наличия в базе ссылок на несуществующие вложения, " +
                            "имеется возможность фильтрации базы данных. Эта функция " +
                            "проверит доступность каждого вложения во всех ответах, " +
                            "и если вложения из него более недоступны, удалит ответ из базы.\n" +
                            "Эта функция может выполняться очень долго! Даже больше суток! " +
                            "Для того, чтобы контролировать процесс выполнения функции, есть команда filterMediaStatus. " +
                            "А если нужно остановить фильтрацию - можно использовать команду filterMediaStop.",
                    "botcmd filterMedia"));

            result.add(new CommandDesc("Просмотреть статус фильтрации базы данных от битого медиа",
                    "Поскольку фильтрация базы данных может длиться очень долго, её состояние можно периодически " +
                            "проверять с помощью этой команды.",
                    "botcmd filterMediaStatus"));

            result.add(new CommandDesc("Остновить фильтрацию базы данных от битого медиа",
                    "Фильтрация длится очень долго. Если тебе надоело ждать, её можно остановить этой командой.",
                    "botcmd filterMediaStop"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String word = commandParser.getWord().toLowerCase();
            if(word.equals("filtermedia"))
                return filterMedia(message);
            if(word.equals("filtermediastatus"))
                return status(message);
            if(word.equals("filtermediastop"))
                return stop(message);
            return "";
        }

        private String filterMedia(final Message message){
            //сообщение в аргументах функции нужно для того, чтобы иметь возможность
            // уведомлять польщователя о прогрессе фильтрации.
            if(filterThread != null){
                return "В данный момент уже происходит фильтрация медиа. " +
                        "Статус фильтрации:\n" + status(message);
            }
            filterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    message.sendAnswer(filter(message));
                }
                private String filter(Message message){
                    try {
                        File fileTmp = new File(applicationManager.getHomeFolder(), "Answer_database_filtered.tmp");
                        PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
                        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
                        timeStarted = System.currentTimeMillis();
                        ArrayList<Long> answersToRemoveId = new ArrayList<>();
                        String line;

                        while ((line = bufferedReader.readLine()) != null) {
                            if(filterThread == null){
                                //фильтрация была остановлена командой
                                bufferedReader.close();
                                fileTmpWriter.close();
                                System.gc();
                                if(fileTmp.delete())
                                    return "Фильтрация базы данных остановлена. Временный файл был успешно уданён.";
                                else
                                    return "Фильтрация базы данных остановлена. Временный файл удалить не удалось.";
                            }

                            if (processedAnswers % 500 == 0)
                                log(". Фильтрация базы данных ...(" + processedAnswers + " уже проверено)");
                            //каждые 5000 ответов система уведомляет пользователя о состоянии фильтрации
                            if (processedAnswers % 5000 == 0)
                                message.sendAnswer(new Answer(status(message)));
                            try {
                                JSONObject jsonObject = new JSONObject(line);
                                AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                                //если вложения этого ответа не прошли проверку, а какой-то из них недоступен
                                if (currentAnswerElement.checkAnswerAttachments()) {
                                    //просто нихуя с ним не делать
                                    removedAnswers ++;
                                    answersToRemoveId.add(currentAnswerElement.getId());
                                }
                                else
                                    fileTmpWriter.println(currentAnswerElement.toJson().toString());
                                processedAnswers ++;
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                failedAnswers ++;
                                log("! Ошибка разбора строки " + processedAnswers + " как ответа из базы.\n" + e.getMessage());
                            }
                            timePing = System.currentTimeMillis();
                        }
                        if (failedAnswers != 0)
                            log("! При загрузке базы ответов возникло ошибок: " + failedAnswers + ".");
                        message.sendAnswer("Поиск битых медиа в базе данных завершён. Всего найдено " + removedAnswers + " " +
                                "ответов, которые содержат битые медиа.\n" +
                                "Сейчас выполняется удаление этих ответов из оперативной памяти...");

                        //удалить ответы в оперативной памяти
                        log(". Удаление ответа в базе (в оперативке)");
                        for (int i = 0; i < answers.size(); i++) {
                            AnswerMicroElement answerMicroElement = answers.get(i);
                            if (answersToRemoveId.contains(answerMicroElement.getId())) {
                                answers.remove(answerMicroElement);
                                i --;
                                if(i < 0)
                                    i = 0;
                            }
                        }
                        message.sendAnswer("Ответы удалены из оперативной памяти. " +
                                "Теперь завершаю сессию и выполняю замену файлов...");
                        //завешить сессию
                        bufferedReader.close();
                        fileTmpWriter.close();
                        System.gc();
                        //Подменить файлы
                        backupsManager.backupDatabase("после фильтрации медиа");
                        if (!fileAnswers.delete())
                            throw new Exception(log("! Не могу удалить текущий файл с базой " + fileAnswers.getName()));
                        if (fileTmp.renameTo(fileAnswers))
                            throw new Exception(log("! Не могу перенести новый файл на место старого!" +
                                    "Проверь, не запущена ли какая-то длительная процедура, типа фильтрации."));
                        String result = "Поздравляю! Фильтрация базы данных выполнена!\n" +
                                "Фильтрация базы данных была начата " + F.getDateTimeString(F.timeMillisToDate(timeStarted)) + "\n" +
                                "Всего ответов в базе: " + totalAnswers + "\n" +
                                "Ответов прошло фильтрацию: " + processedAnswers + "\n" +
                                "Ответов с ошибками (потеряно): " + failedAnswers + "\n" +
                                "Ответов было удалено: " + removedAnswers + "\n";
                        stop(message);
                        return result;
                    }
                    catch (Exception e){
                        return e.getMessage();
                    }
                }
            });
            filterThread.start();
            return "Фильтрация медиа начата. Я буду присылать тебе периодически отчёты о состоянии фильтрации.";
        }
        private String status(Message message){
            if(filterThread == null)
                return "Фильтрация базы данных сейчас не работает.";
            String result = "Происходит фильтрация базы данных.\n";
            result += "Фильтрация базы данных была начата " + F.getDateTimeString(F.timeMillisToDate(timeStarted)) + "\n";
            long now = System.currentTimeMillis();
            long timeFromLastAction = now - timePing;
            if(timeFromLastAction > 120000)
                result += "Кажется, фильтрация зависла. Последняя активность была замечена " +
                        F.getDateTimeString(F.timeMillisToDate(timePing)) + "\n";
            else
                result += "Фильтрация продолжается. Последняя активность была замечена " +
                        F.getDateTimeString(F.timeMillisToDate(timePing)) + "\n";
            result += "Всего ответов в базе: " + totalAnswers + "\n";
            result += "Ответов прошло фильтрацию: " + processedAnswers + "\n";
            result += "Ответов с ошибками (потеряно): " + failedAnswers + "\n";
            result += "Ответов было удалено: " + removedAnswers + "\n";
            float percent = ((float)processedAnswers / (float) totalAnswers) * 100f;
            result += "Фильтрация выполнена на " + Math.round(percent) + "%";
            return result;
        }
        private String stop(Message message){
            String status = status(message);
            filterThread = null;
            timeStarted = 0;
            timePing = 0;
            totalAnswers = 0;
            processedAnswers = 0;
            removedAnswers = 0;
            return "Фильтрация ответов останавливается...\n" +
                    "Состояние фильтрации перед остановкой:\n" +
                    status;
        }
    }
    private class ReplaceDatabase extends CommandModule{
        public ReplaceDatabase(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Загрузить базу данных в бота",
                    "Перезаписать текущую базу данных на базу, полученную в виде документа.\n" +
                            "Данная функция заменит текущую базу данных новой.\n" +
                            "Чтобы эта команда сработала, присылай файл с " +
                            "базой данных в том же сообщении что и команду.",
                    "botcmd ReplaceDatabase"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("replacedatabase")){
                try {
                    int recordsBefore = answers.size();

                    log(". Импорт базы данных...\n");
                    if (message.getAttachments().size() == 0)
                        return "Не получен файл базы данных для замены.";
                    if (message.getAttachments().size() > 0)
                        return "Для нормальной работы команды нужно, " +
                                "чтобы в сообщении было только одно вложение. (только один документ, который база данных)";
                    Attachment attachment = message.getAttachments().get(0);
                    if (!attachment.getType().equals(Attachment.TYPE_DOC))
                        return "Вместе с командой нужно присылать документ с базой данных, именно в виде документа.";
                    File newDatabase = attachment.getFile();//скачать документ
                    if (newDatabase == null)
                        return "Не удаётся загрузить документ.";
                    message.sendAnswer(new Answer("База данных была скачана, сейчас будет производиться замена файла..."));
                    backupsManager.backupDatabase("перед заменой базы данных командой");
                    if(fileAnswers.exists() && !fileAnswers.delete())
                        return "Не удаётся удалить старый файл базы данных. Возможно, с ним " +
                                "сейчас работает какая-то программа, либо команда.";
                    //на этом месте старый файл уже удалён
                    boolean success = F.copyFile(newDatabase, fileAnswers);
                    if(!success)
                        return "Не удаётся копировать новый файл базы данных на место старого.";
                    message.sendAnswer(new Answer("База заменена. Выполняется обработка новых ответов..."));
                    load();
                    int recordsAfter = answers.size();
                    return "База данных заменена. До замены была база данных на " + recordsBefore + " ответов, " +
                            "после замены количество ответов стало " + recordsAfter + ".\n" +
                            "Если понадобится вернуть старую базу, восстанови резервную копию.";
                }
                catch (Exception e){
                    return e.getMessage();
                }

            }
            return "";
        }
    }
    private class ImportDatabase extends CommandModule{
        public ImportDatabase(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Импортировать ответы в базу данных бота",
                    "Добавить в базу ответы из базы, полученной в виде документа\n" +
                            "Данная команда добавит в текущую базу новые ответы, старые ответы останутся без изменений.\n" +
                            "Чтобы эта команда сработала, присылай файл с " +
                            "базой данных в том же сообщении что и команду.\n" +
                            "Все новые ответы добавленные в базу получат новые ID, чтобы предотвратить повторы ID.",
                    "botcmd ImportDatabase"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("importdatabase")){
                try {
                    log(". Импорт базы данных...\n");
                    if (message.getAttachments().size() == 0)
                        return "Не получен файл базы данных для импорта.";
                    if (message.getAttachments().size() > 0)
                        return "Для нормальной работы команды нужно, " +
                                "чтобы в сообщении было только одно вложение. (только один документ, который база данных)";
                    Attachment attachment = message.getAttachments().get(0);
                    if (!attachment.isDoc())
                        return "Вместе с командой нужно присылать документ с базой данных, именно в виде документа.";
                    int recordsBefore = answers.size();
                    File newDatabase = attachment.getFile();//скачать документ
                    if (newDatabase == null)
                        return "Не удаётся загрузить документ.";
                    message.sendAnswer(new Answer("База данных была скачана, сейчас будет производиться импорт файла..."));
                    importAnswers(newDatabase);
                    int recordsAfter = answers.size();
                    return "База данных импортирована. \n" +
                            "Ответов до импорта: " + recordsBefore + "\n" +
                            "Ответов после импорта: " + recordsAfter + "\n" +
                            "Если понадобится вернуть старую базу, восстанови резервную копию.\n" +
                            "Обрати внивание: если твоя новая база содержала такие же ответы, которые уже были у тебя раньше, " +
                            "то теперь у тебя в базе есть дубликаты ответов (одинаковые ответы). " +
                            "Это не очень страшно, но они очень замедляют бота, из-за чего он может отвечать долго.\n" +
                            "Если ты хочешь удалить дубликаты ответов, используй команду ClearDuplicates.";
                }
                catch (Exception e){
                    return e.getMessage();
                }

            }
            return "";
        }
    }
    private class ClearDuplicates extends CommandModule{
        private Thread filterThread = null;//when filtering is in progress, make not null
        private long timeStarted = 0; //время когда была начата фильтрация
        private long timePing = 0; //это число показывает момент в который
        // было последнее действие от потока оно
        // позволяет определить завис поток или нет
        private long totalAnswers = 0; //эта пара переменных позволит определить прогресс фильтрации базы данных
        private long processedAnswers = 0;
        private long duplicateAnswers = 0;
        private long removedAnswers = 0;

        public ClearDuplicates(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Очистить дубликаты ответов",
                    "Дубликаты - это когда в нескольких ответах в базе содержатся одинаковые вопросы и одинаковые ответы. " +
                            "При этом дубликаты имеют разные ID (потому что в базе нельзя иметь два ответа с одинаковыми ID)\n" +
                            "Дубликаты в базе ответов приводят к бесполезному использованию оперативной памяти " +
                            "и к дольшему подбору ответа.\n" +
                            "Дубликаты могут возникать, например, при импорте баз данных." +
                            "Команда очистки дубликатов может работать очень долго. " +
                            "Время её работы очень зависит от размера базы данных.\n" +
                            "Например, очистка 100 000 ответов от дубликатов может длиться около 7-9 часов. " +
                            "Команда работает так долго потому, что для того чтобы найти дубликаты, " +
                            "нужно сравнить каждый ответ с каждым, а это очень много..\n" +
                            "Очистка дубликатов происходит в два этапа. На первом этапе мы ищем дубликаты, а на втором - удаляем.\n" +
                            "В процессе работы команды тебе будут отправлены сообщения о состоянии.\n" +
                            "Чтобы остановить работающую команду, используй команду ClearDuplicatesStop\n" +
                            "Чтобы узнать, на сколько процедура поиска дубликатов завершена, используй команду ClearDuplicatesStatus.",
                    "botcmd ClearDuplicates"));
            result.add(new CommandDesc("Остановить очистку дубликатов",
                    "Остановить работающую программу очистки дубликатов.\n" +
                            "Программе очистки будет отправлено требование остановить поиск. " +
                            "Иногда остановка поиска может занять несколько минут.",
                    "botcmd ClearDuplicatesStop"));
            result.add(new CommandDesc("Статус очистки дубликатов",
                    "Эта команда позволит узнать, насколько продвинулась программа очистки дубликатов.",
                    "botcmd ClearDuplicatesStatus"));
            return result;
        }

        @Override
        public String processCommand(final Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String word = commandParser.getWord().toLowerCase();
            if(word.equals("clearduplicates")) {
                if(filterThread != null)
                    return "Очистка дубликатов уже работает. Статус задачи:\n" + status();
                filterThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        clearDuplicates(message);
                    }
                });
                filterThread.start();
                return "Очистка дубликатов начата.\n" +
                        "Это очень длительная команда, и в зависимости от " +
                        "размера базы она может длиться от 1 минуты до 9 часов.\n" +
                        "Чтобы проверить статус очистки дубликатов, есть команда ClearDuplicatesStatus\n" +
                        "Чтобы остановить очистку дубликатов, есть команда ClearDuplicatesStop";
            }
            if(word.equals("clearduplicatesstatus"))
                return status();
            if(word.equals("clearduplicatesstop")){
                String status = status();
                stop();
                return "Текущий статус задачи: \n" + status + "\n\n" +
                        "Отправлена команда остановки. Когда очистка остановится, ты получишь сообщение.";
            }
            return "";
        }

        //блокирующая функция, которую следует вызывать в отдельном потоке
        private void clearDuplicates(Message message){
            ArrayList<Long> toDelete = new ArrayList<>();
            totalAnswers = answers.size();
            timeStarted = System.currentTimeMillis();


            for (int i = 1; i < answers.size(); i++) {
                timePing = System.currentTimeMillis();
                AnswerMicroElement toCheck = answers.get(i);
                //проверить дубликат ли. если да - добавить в список на удаление
                for(int j=0; j<i; j++){
                    if(toCheck.isSame(answers.get(j))){
                        toDelete.add(toCheck.getId());
                        duplicateAnswers ++;
                        break;
                    }
                }
                processedAnswers ++;
                if(filterThread == null){
                    //остановочка
                    message.sendAnswer("Очистка дубликатов остановлена.\n" + status());
                    break;
                }
            }
            message.sendAnswer("Поиск дубликатов завершён. Удаление дубликатов из базы...\n" + status());
            try {
                removedAnswers = removeAnswer(toDelete);
                message.sendAnswer("Дубликаты удалены!\n" + status());
            }
            catch (Exception e){
                message.sendAnswer("Ошибка удаления ответов из базы: " + e.getMessage());
            }
            stop();
        }
        private String status(){
            if(filterThread == null)
                return "Очистка дубликатов не запущена.";
            return "Прогресс очистки: " + (100 * processedAnswers / totalAnswers) + "%\n" +
                    "Очистка была начата: " + F.getDateTimeString(F.timeMillisToDate(timeStarted)) + "\n" +
                    "Последняя активность была: " + F.getDateTimeString(F.timeMillisToDate(timePing)) + "\n" +
                    "Всего ответов: " + totalAnswers + "\n" +
                    "Проверено ответов: " + processedAnswers + "\n" +
                    "Найдено дубликатов: " + duplicateAnswers + "\n" +
                    "Удалено дубликатов: " + removedAnswers + "\n";
        }
        @Override
        public void stop(){
            filterThread = null;
            timeStarted = 0;
            timePing = 0;
            totalAnswers = 0;
            processedAnswers = 0;
            duplicateAnswers = 0;
            removedAnswers = 0;
        }
    }

    private class AnswerCandidate{
        //Массив элементов этого класса будет результатом подбора ответов.
        //Функция должна возвращать 20 варивантов отевта
        AnswerMicroElement answer;
        double similarity;

        public AnswerCandidate(AnswerMicroElement answer, double similarity) {
            this.answer = answer;
            this.similarity = similarity;
        }

        @Override
        public String toString() {
            return answer.toString() +
                    ", similarity=" + similarity;
        }
    }
    private class AnswerUsageCounter extends CommandModule{
        //// вести статистику частоты использования ответов
        // Вести эти данные в оперативке, однако, периодически записывать данные в файл.
        // добавить в файле поле для того, чтобы вести учёт частоты использования
        private HashMap<Long, Integer> timesUsed = new HashMap<>();
        private File timesUsedFile = new File(applicationManager.getHomeFolder(), "answerUsageCounter");
        private int refreshCounter = 0;//Этот счётчик надо для того чтобы данные в файл писались не всегда, а каждые 100 инкрементов

        AnswerUsageCounter(ApplicationManager applicationManager) {
            super(applicationManager);
            readFromFile();
            childCommands.add(new ShowStat(applicationManager));
            childCommands.add(new SyncStat(applicationManager));
        }
        void increment (long answerId){
            if(timesUsed.containsKey(answerId))
                timesUsed.put(answerId, timesUsed.get(answerId) + 1);
            else
                timesUsed.put(answerId, 1);

            refreshCounter ++;
            if(refreshCounter % 100 == 0){
                writeToFile();
                if(refreshCounter > 1000) {
                    try {
                        syncWithDatabase();
                        refreshCounter = 0;
                    }catch (Exception e){}
                }
            }
        }

        private void readFromFile(){
            try {
                if(!timesUsedFile.exists()) {
                    log("Файла " + timesUsedFile.getName() + " не существует. Пропускаем его загрузку.");
                    return;
                }
                BufferedReader bufferedReader = new BufferedReader(new FileReader(timesUsedFile));
                String line;
                int lineNumber = 0;
                int errors = 0;
                timesUsed.clear();
                while ((line = bufferedReader.readLine()) != null) {
                    lineNumber ++;
                    if(lineNumber%23 == 0)
                        log(". Статистика использования ответов загружается... (" + lineNumber + " загружено)");
                    try {
                        JSONObject jsonObject = new JSONObject(line);
                        timesUsed.put(jsonObject.getLong("id"), jsonObject.getInt("count"));
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        errors ++;
                        log("! Ошибка разбора строки " + lineNumber + " как элемента статистики.\n" + e.getMessage());
                    }
                }
                bufferedReader.close();
                log(". Загружено " + answers.size() + " счётчиков статистики ответов.");
                if(errors != 0)
                    log("! При загрузке базы ответов возникло ошибок: " + errors + ".");
            }
            catch (Exception e){
                e.printStackTrace();
                log("! Ошибка загрузки модуля работы с базой ответов: " + e.getMessage());
            }
        }
        private void writeToFile(){
            try {
                PrintWriter fileTmpWriter = new PrintWriter(timesUsedFile);
                int lineNumber = 0;
                Set<Map.Entry<Long, Integer>> set = timesUsed.entrySet();
                Iterator<Map.Entry<Long, Integer>> iterator = set.iterator();
                while(iterator.hasNext()){
                    lineNumber++;
                    if (lineNumber % 23 == 0)
                        log(". Сохранение статистики в файл (" + lineNumber + " уже сохранено)");
                    Map.Entry<Long, Integer> entry = iterator.next();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", entry.getKey());
                    jsonObject.put("count", entry.getValue());
                    fileTmpWriter.println(jsonObject.toString());
                }
                //завешить сессию
                fileTmpWriter.close();
            }catch (Exception e){
                log("! Ошибка сохранения статистических данных: " + e.getMessage() + ".");
            }
        }
        private void syncWithDatabase() throws Exception{
            /*Эта функция берёт данные из оперативки и переносит их в базу ответов.
            Массив в опеативке после этого остаётся пустым.
            * */
            File fileTmp = new File(applicationManager.getHomeFolder(), "Answer_database.tmp");
            PrintWriter fileTmpWriter = new PrintWriter(fileTmp);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
            String line;
            int lineNumber = 0;
            int errors = 0;

            while ((line = bufferedReader.readLine()) != null) {
                lineNumber++;
                if (lineNumber % 1000 == 0)
                    log(". Обновление статичтики в базе (" + lineNumber + " уже пройдено)");
                try {
                    JSONObject jsonObject = new JSONObject(line);
                    AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                    if (timesUsed.containsKey(currentAnswerElement.getId())){
                        currentAnswerElement.setTimesUsed(
                                currentAnswerElement.getTimesUsed() +
                                        timesUsed.get(currentAnswerElement.getId()));
                    }
                    fileTmpWriter.println(currentAnswerElement.toJson().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    errors++;
                    log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                }
            }
            if (errors != 0)
                log("! При обновлении базы ответов возникло ошибок: " + errors + ".");

            //завешить сессию
            bufferedReader.close();
            fileTmpWriter.close();
            timesUsed.clear();
            System.gc();
            //Подменить файлы
            backupsManager.backupDatabase("после обновления статистики");
            if (!fileAnswers.delete())
                throw new Exception(log("! Не могу удалить текущий файл с базой " + fileAnswers.getName()));
            if (fileTmp.renameTo(fileAnswers))
                throw new Exception(log("! Не могу перенести новый файл на место старого!" +
                        "Проверь, не запущена ли какая-то длительная процедура, типа фильтрации."));
            writeToFile();
        }


        class ShowStat extends CommandModule{
            public ShowStat(ApplicationManager applicationManager) {
                super(applicationManager);
            }

            @Override
            public ArrayList<CommandDesc> getHelp() {
                ArrayList<CommandDesc> result = new ArrayList<>();
                result.add(new CommandDesc("Топ используемых ответов",
                        "Вывести топ ответов в базе по частоте использования.\n" +
                                "Так можно узнать, сколько раз бот использовал какой либо ответ." +
                                "За раз можно вывести до 50 ответов.",
                        "botcmd ShowStat <количество элементов в топе> <Сдвиг от начала топа>"));
                return result;
            }

            @Override
            public String processCommand(Message message) {
                CommandParser commandParser = new CommandParser(message.getText());
                String keyword = commandParser.getWord();
                if(keyword.toLowerCase().equals("showstat")){
                    try {
                        int count = commandParser.getInt();
                        int offset = commandParser.getInt();
                        if(count < 1)
                            count = 20;
                        if(count > 50) {
                            count = 50;
                            message.sendAnswer("Максимум за раз можно вывести до 50 ответов. Будет выведено 50 ответов.");
                        }
                        if(offset < 0)
                            offset = 0;
                        if(offset + count > 10000){
                            offset = 10000 - count;
                            message.sendAnswer("Загрузка дальних элементов топа требует очень много памяти. " +
                                    "Максимально возможное место топа 10000. Будут показаны последние " + count + " ответов " +
                                    "до 10000.");
                        }
                        else if(offset + count > 2000)
                            message.sendAnswer("Загрузка дальних элементов топа требует очень много памяти. " +
                                    "Если эта команда вызовет ошибку, попробуй сдвиг и количество поменьше.");
                        AnswerElement[] top = getTopAnswers(count, offset, message);
                        if(top.length == 0)
                            return "В топе нет ни одного элемента.";
                        String result = "Список "+top+" самых часто употребляемых ответов";
                        if(offset == 0)
                            result += ":\n";
                        else
                            result += " со сдвигом на " + offset + " ответов:\n";
                        for (AnswerElement answerElement:top)
                            result += answerElement.toShortString() + "\n";
                        return result;
                    }
                    catch (Exception e){
                        return "Ошибка: " + e.getMessage() + "\n" +
                                "Формат команды:\n"+
                                F.commandDescToText(getHelp().get(0));
                    }
                }
                return "";
            }

            private AnswerElement[] getTopAnswers(int count, int offset, Message message) throws Exception{
                AnswerElement[] topAnswers = getTopAnswers(count + offset, message);
                AnswerElement[] result = new AnswerElement[count];
                /*
                * +                      -
                * ############# ###########
                * ####
                * ------------
                * +          -
                *
                * */

                int startIndex = 0;
                if(topAnswers.length > count)
                    startIndex = topAnswers.length - count - 1;
                if(startIndex + count > topAnswers.length)
                    message.sendAnswer("Всего в топе " + topAnswers.length + " ответов. " +
                            "Будут показаны последние " + count + ".");
                for (int i = startIndex; i < topAnswers.length; i++)
                    result[i - startIndex] = topAnswers[i];
                return result;
            }
            private AnswerElement[] getTopAnswers(int count, Message message) throws Exception{
                /*
                * Вот тут перед нами стоит сложная задача.
                * Нужно отсортировать ВСЮ БАЗУ так, чтобы отобрать топ,
                * при этом быстро и не заняв всю оперативку.
                * Предположим, что неиспользуемых ответов у нас нет
                * ! Перед этим обязательно надо синхронизировать базу.
                *
                * можно хранить массив нужного нам размера.
                * В начале массива находится самый большой элемент.
                *
                * Делаем один проход по базе ответов.
                * Во время этого прохода проверяем не меньше ли число ответа меньшего числа топа.
                 * Если текущий ответ менее популярен чем нижнее число топа или такой же - пропускаем.
                 * Если он более популярен чем нижнее число топа, начинаем с начала в конец искать в массиве
                 * ячейку, которая менее популярна.
                 * После этого выполняем сдвиг с этой ячейки в конец.
                 * Вставляем наш элемент в ячейку.*/

                message.sendAnswer("Производится синхрорнизация базы статистики и базы ответов. Подождите....");
                syncWithDatabase();
                message.sendAnswer("Выборка из базы данных. Подождите....");

                AnswerElement[] topElements = new AnswerElement[count];

                BufferedReader bufferedReader = new BufferedReader(new FileReader(fileAnswers));
                String line;
                int lineNumber = 0;
                int errors = 0;

                while ((line = bufferedReader.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber % 1000 == 0)
                        log(". Сбор статистической информации (" + lineNumber + " уже пройдено)");
                    try {
                        JSONObject jsonObject = new JSONObject(line);
                        AnswerElement currentAnswerElement = new AnswerElement(jsonObject);
                        if(topElements[topElements.length-1] == null ||
                                topElements[topElements.length-1].getTimesUsed() < currentAnswerElement.getTimesUsed()){
                            //добавить в топ
                            for (int i = 0; i < topElements.length; i++) {
                                if(topElements[i].getTimesUsed() < currentAnswerElement.getTimesUsed()){
                                    //выполнить сдвиг
                                    for (int j = topElements.length-1; j > i; j--)
                                        topElements[j] = topElements[j-1];
                                    //записать элемент в топ
                                    topElements[i] = currentAnswerElement;
                                    //выйти из цикла
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        errors++;
                        log("! Ошибка разбора строки " + lineNumber + " как ответа из базы.\n" + e.getMessage());
                    }
                }
                if (errors != 0)
                    log("! При обновлении базы ответов возникло ошибок: " + errors + ".");

                //завешить сессию
                bufferedReader.close();
                timesUsed.clear();
                System.gc();
                return topElements;
            }
        }
        class SyncStat extends CommandModule{
            public SyncStat(ApplicationManager applicationManager) {
                super(applicationManager);
            }

            @Override
            public ArrayList<CommandDesc> getHelp() {
                ArrayList<CommandDesc> result = new ArrayList<>();
                result.add(new CommandDesc("Синхронизировать статистику с базой",
                        "Статистика использования ответов вносится в базу в 3 этапа.\n" +
                                "Сначала информация сохраняется в оперативной памяти. " +
                                "Каждые 100 ответов данные с памяти сохраняются во временный файл статистики. " +
                                "А каждые 1000 ответов статистическая информация обновляется в свойствах ответов в базе данных.\n" +
                                "Эта команда позволяет обновить информацию в свойствах ответов в базе данных.",
                        "botcmd SyncStat"));
                return result;
            }

            @Override
            public String processCommand(Message message) {
                CommandParser commandParser = new CommandParser(message.getText());
                String keyword = commandParser.getWord();
                if(keyword.toLowerCase().equals("syncstat")){
                    try {
                        int cnt = timesUsed.size();
                        message.sendAnswer("Начинаю синхронизацию...");
                        syncWithDatabase();
                        return "Синхронизация выполнена. Обновлены данные " + cnt + " ответов.";
                    }
                    catch (Exception e){
                        return "Ошибка: " + e.getMessage() + "\n" +
                                "Формат команды:\n"+
                                F.commandDescToText(getHelp().get(0));
                    }
                }
                return "";
            }
        }
    }
    private class BackupsManager extends CommandModule{

        public BackupsManager(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        public void backupDatabase(String reason){
            backupDatabase(reason, true);
        }
        public void backupDatabase(String reason, boolean clearOld){
//  Переносим файл с базой: answer_database.bin в папку sdcard\backups\database\backup_yyyy-MM-dd_HH-mm.bin
//  * - Чистим кэш. Оставляем только (database_backups, 50) последних бекапов
            if(!fileAnswers.exists()){
                log("! Не могу сохранить резервную копию базы данных, потому что её на карте тупо нет.");
                return;
            }
            //sdcard\backups\database\backup_yyyy-MM-dd_HH-mm.bin
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ENGLISH);
            String folderWithBackups = applicationManager.getHomeFolder() + File.separator +
                    "backups" + File.separator +
                    "database";
            String newAddress = folderWithBackups + File.separator +
                    "backup_" + reason.replace(" ", "_") + "_" + sdf.format(new Date()) + ".bin";
            File backupFile = new File(newAddress);
            if(!backupFile.getParentFile().exists())
                if(!backupFile.getParentFile().mkdir()){
                    log("! Не могу сохранить резервную копию базы данных, потому что не могу создать папку для неё.");
                    return;
                }
            if(!fileAnswers.renameTo(backupFile)){
                log("! Не могу сохранить резервную копию базы данных, потому что не могу пененести оригинальный файл в папку с бекапами.");
                return;
            }
            if(clearOld)
                backupsClear();
        }

        private void backupsClear(){
            //Удалить старые бекапы базы данных
            log(". Очистка старых бекапов...");
            int numberOfBackups = applicationManager.getParameters().get(
                    "backups_number",
                    50,
                    "Количество бекапов.",
                    "Количество резервных копий базы данных. " +
                            "Резервные копии создаются при каждом изменении базы ответов.\n" +
                            "Когда количество резервных копий превышает это число, старые резервные копии автоматически удаляются.");
            log(". Нужно оставить максимум " + numberOfBackups + " бекапов.");
            String folderWithBackupsAddress = applicationManager.getHomeFolder() + File.separator +
                    "backups" + File.separator +
                    "database";
            log(". Папка с бекапами: " + folderWithBackupsAddress);
            File folderWithBackups = new File(folderWithBackupsAddress);
            if(!folderWithBackups.isDirectory()) {
                log(". Папка с бекапами не создана.");
                return;
            }
            File[] files = folderWithBackups.listFiles();
            if(files.length == 0) {
                log(". Папка с бекапами пустая.");
                return;
            }
            if(files.length < numberOfBackups){
                log(". "+numberOfBackups + " бекапов ещё не набралось.");
                return;
            }
            log(". Сортировка файлов бекапов...");
            for (int i=0; i<files.length-1; i++){
                for (int j = i; j < files.length-1; j++) {
                    //если в начале идёт файл который старше
                    if(files[j].lastModified() < files[j+1].lastModified()){
                        File tmp = files[j];
                        files[j] = files[j+1];
                        files[j+1] = tmp;
                    }
                }
            }
            log(". Удаление старых бекапов...");
            int deleted = 0;
            for (int i=numberOfBackups; i<files.length; i++){
                if(files[i].delete())
                    deleted++;
            }
            log(". Удалено бекапов: " + deleted);
        }
        private File[] getBackupsList() throws Exception{
            String folderWithBackupsAddress = applicationManager.getHomeFolder() + File.separator +
                    "backups" + File.separator +
                    "database";
            log(". Папка с бекапами: " + folderWithBackupsAddress);
            File folderWithBackups = new File(folderWithBackupsAddress);
            if(!folderWithBackups.isDirectory()) {
                log(". Папка с бекапами не создана.");
                throw new Exception("Папка с бекапами отсутствует: " + folderWithBackupsAddress);
            }
            File[] files = folderWithBackups.listFiles();
            if(files.length == 0) {
                log(". Папка с бекапами пустая.");
                throw new Exception("Папка с бекапами пустая: " + folderWithBackupsAddress);
            }
            return files;
        }
        private void restoreBackup(File file) throws Exception{
            backupDatabase("Перед восстановленем резервной копии ["+file.getName()+"]", false);
            if(!file.exists())
                throw new Exception("Полученный файл бекапа не существует.");
            if(fileAnswers.exists() && !fileAnswers.delete())
                throw new Exception("Не могу удалить файл текущей базы, поэтому не могу восстановить бекап.");
            if(F.copyFile(file.getPath(), fileAnswers.getPath()))
                throw new Exception("Не могу удалить файл бекапа!\n" +
                        file.getPath() + " -> " + fileAnswers.getPath());
            load();
            log(". Резервная копия базы ответов " + file.getName() + " восстановлена.");
        }
    }
}
