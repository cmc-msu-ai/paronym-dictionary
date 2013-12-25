package db;

import java.sql.*;

import java.lang.*;
import java.io.*;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

/**
 * Класс, обеспечивающий работу с базой данных.
 */
public class Database {

    /**
     * Соединение
     */
    Connection conn;

    /**
     * Конструктор объекта Database.
     * Выполняет загрузку JDBC драйвера и установку соединения с базой данных.
     * @param db_file_name_prefix имя базы данных. Если БД с таким именем не существует, она будет создана.
     * @throws Exception ошибка работы с БД
     */
    public Database(String db_file_name_prefix) throws Exception {

        System.out.println("Connecting to Database " + db_file_name_prefix);

        // Load the HSQL Database Engine JDBC driver
        // hsqldb.jar should be in the class path or made part of the current jar
        Class.forName("org.hsqldb.jdbcDriver");

        // connect to the database.   This will load the db files and start the
        // database if it is not alread running.
        // db_file_name_prefix is used to open or create files that hold the state
        // of the db.
        // It can contain directory names relative to the
        // current working directory
        conn = DriverManager.getConnection("jdbc:hsqldb:"
                                           + db_file_name_prefix,    // filenames
                                           "sa",                     // username
                                           "");                      // password
    }

    /**
     * Создание базы данных словаря паронимов.
     * @throws SQLException ошибка работы с БД
     */
    public void createDB() throws SQLException {

        //удалить старые данные
        update("DROP TABLE words_morphs IF EXISTS CASCADE");
        update("DROP TABLE paronyms IF EXISTS CASCADE");
        update("DROP TABLE words IF EXISTS CASCADE");
        update("DROP TABLE morphs IF EXISTS CASCADE");
        update("DROP TABLE roots IF EXISTS CASCADE");
        update("DROP TABLE part_codes IF EXISTS CASCADE");
        update("DROP TABLE gen_codes IF EXISTS CASCADE");
        update("DROP TABLE num_codes IF EXISTS CASCADE");
        update("DROP TABLE anim_codes IF EXISTS CASCADE");
        update("DROP TABLE morph_codes IF EXISTS CASCADE");

        //не различать регистр
        execute("set ignorecase true");

        //создать таблицы
        update("create table part_codes(" +
                "code tinyint," +
                "str varchar(16)," +
                "primary key(code)) ");
        update("create table num_codes(" +
                "code tinyint," +
                "str varchar(16)," +
                "primary key(code)) ");
        update("create table gen_codes(" +
                "code tinyint," +
                "str varchar(16)," +
                "primary key(code)) ");
        update("create table anim_codes(" +
                "code tinyint," +
                "str varchar(16)," +
                "primary key(code)) ");
        update("create table morph_codes(" +
                "code tinyint," +
                "str varchar(16)," +
                "primary key(code)) ");
        update(
                "CREATE cached TABLE words( " +
                        "id INTEGER IDENTITY, " +
                        "word VARCHAR(64), " +
                        "part TINYINT," +
                        "gen TINYINT," +
                        "num TINYINT," +
                        "anim TINYINT," +
                        "aot_id INTEGER," +
                        "PRIMARY KEY (id)," +
                        "foreign key (part) references part_codes(code)," +
                        "foreign key (gen) references gen_codes(code)," +
                        "foreign key (num) references num_codes(code)," +
                        "foreign key (anim) references anim_codes(code))"
        );
        update(
                "CREATE cached  TABLE morphs(" +
                        "id_m INTEGER IDENTITY," +
                        "morph VARCHAR(64)," +
                        "type TINYINT," +
                        "PRIMARY KEY (id_m)," +
                        "foreign key (type) references morph_codes(code))"
        );
        update("CREATE cached TABLE roots(" +
                "id1 INTEGER," +
                "id2 INTEGER," +
                "part TINYINT," +
                "PRIMARY KEY(id1,id2,part)," +
                "FOREIGN KEY (id1) REFERENCES morphs (id_m)," +
                "FOREIGN KEY (id2) REFERENCES morphs (id_m))"
        );
        update(
                "CREATE cached TABLE words_morphs(" +
                        "id INTEGER," +
                        "n TINYINT," +
                        "id_m INTEGER," +
                        "PRIMARY KEY (id,n)," +
                        "FOREIGN KEY (id) REFERENCES words (id)," +
                        "FOREIGN KEY (id_m) REFERENCES morphs (id_m))"
        );
        update(
                "CREATE cached TABLE paronyms(" +
                        "id1 INTEGER," +
                        "id2 INTEGER," +
                        "dlcode TINYINT," +
                        "dmcode INTEGER," +
                        "q DOUBLE," +
                        "PRIMARY KEY(id1,id2)," +
                        "FOREIGN KEY (id1) REFERENCES words (id)," +
                        "FOREIGN KEY (id2) REFERENCES words (id))"
        );


        update(
                "create index words_word_Ind on words (word)"
        );
        update(
                "create index morphs_morph_Ind on morphs (morph)"
        );
        update(
                "create index words_aot_id_Ind on words (aot_id)"
        );

    }

    /**
     * Завершение работы с базой данных.
     * @throws SQLException ошибка работы с БД
     */
    public void shutdown() throws SQLException {

        Statement st = conn.createStatement();
        System.out.println("Shutting down Database");
        // db writes out to files and performs clean shuts down
        // otherwise there will be an unclean shutdown
        // when program ends
        st.execute("SHUTDOWN");
        conn.close();    // if there are no other open connection
    }

    /**
     * Запрос к базе данных. Результат направляется в поток стандартного вывода.
     * @param expression SQL-запрос
     * @throws SQLException ошибка работы с БД
     */
    public synchronized void query(String expression) throws SQLException {

        Statement st;
        ResultSet rs;

        st = conn.createStatement();
        rs = st.executeQuery(expression);
        dump(rs);
        st.close();
    }

    /**
     * Запрос к базе данных.
     * @param expression SQL-запрос
     * @return Результат запроса
     * @throws SQLException ошибка работы с БД
     */
    public synchronized ResultSet queryResult(String expression) throws SQLException {

        Statement st;
        ResultSet rs;

        st = conn.createStatement();
        rs = st.executeQuery(expression);
        st.close();
        return rs;
    }

    /**
     * Выполнить запрос и вернуть число, содержащееся в 1ой колонке 1ой строки результата.
     * Удобно использовать для запросов вида "SELECT id ..." или "CALL IDENTITY()"
     * @param expression SQL-запрос
     * @return число, содержащееся в 1ой колонке 1ой строки результата, либо -1
     * @throws SQLException ошибка работы с БД
     */
    public synchronized int queryAndGetId(String expression) throws SQLException {

        int k = -1;
        Statement st;
        ResultSet rs;

        st = conn.createStatement();
        rs = st.executeQuery(expression);
        if (rs.next()) {
            k = rs.getInt(1);
        }

        st.close();
        return k;
    }


    /**
     * Модификация базы данных.
     * @param expression SQL-выражение
     * @throws SQLException ошибка работы с БД
     */
    public synchronized void update(String expression) throws SQLException {
        Statement st;

        st = conn.createStatement();    // statements
        int i = st.executeUpdate(expression);    // run the query
        if (i == -1) {
            System.out.println("db error : " + expression);
        }

        st.close();
    }

    /**
     * Выполнить команду
     * @param command команда
     * @throws SQLException ошибка работы с БД
     */
    public synchronized void execute(String command) throws SQLException {
        Statement st = conn.createStatement();    // statements
        st.execute(command);    // run command
        st.close();
    }

    /**
     * Вывести результат запроса к базе данных в поток стандартного вывода
     * @param rs результат запроса
     * @throws SQLException ошибка работы с БД
     */
    public static void dump(ResultSet rs) throws SQLException {

        // the order of the rows in a cursor
        // are implementation dependent unless you use the SQL ORDER statement
        ResultSetMetaData meta   = rs.getMetaData();
        int               colmax = meta.getColumnCount();
        int               i;
        Object            o = null;

        for (; rs.next(); ) {
            for (i = 0; i < colmax; ++i) {
                o = rs.getObject(i + 1);    // Is SQL the first column is indexed

                // with 1 not 0
                if (o != null) {
                    System.out.print(o.toString() + " ");
                } else {
                    System.out.print('-');
                }
            }

            System.out.println(" ");
        }
    }                                       //void dump( ResultSet rs )

    /**
     * Тестовое создание базы данных
     * @param args args[0] - имя БД
     */
    public static void main(String[] args) {

        Database db;

        try {
            db = new Database(args[0]);
            db.createDB();
            db.shutdown();
        } catch (Exception ex1) {
            ex1.printStackTrace();
        }

    }

}