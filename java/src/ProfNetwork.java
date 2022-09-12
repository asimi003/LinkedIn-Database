/*
 * Carlos Miranda - 862246355
 * Angelica Simityan - 862220199
 *
 * Template JAVA User Interface
 * =============================
 *
 * Database Management Systems
 * Department of Computer Science &amp; Engineering
 * University of California - Riverside
 *
 * Target DBMS: 'Postgres'
 *
 */


import java.io.*;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * This class defines a simple embedded SQL utility class that is designed to
 * work with PostgreSQL JDBC drivers.
 */
public class ProfNetwork {

    // reference to physical database connection.
    private Connection _connection = null;
    private static final boolean onCampus = false;

    // handling the keyboard inputs through a BufferedReader
    // This variable can be global for convenience.
    static BufferedReader in = new BufferedReader(
            new InputStreamReader(System.in));


    /**
     * Creates a new instance of Messenger
     *
     * @param dbname the name of the database
     * @param dbport the port of the database
     * @param user   the user name used to login to the database
     * @param passwd the user login password
     * @throws java.sql.SQLException when failed to make a connection.
     */
    public ProfNetwork(String dbname, String dbport, String user, String passwd) throws SQLException {

        System.out.print("Connecting to database...");

        try {
            // constructs the connection URL
            String url = "jdbc:postgresql://localhost:" + dbport + "/" + dbname;
            System.out.println("Connection URL: " + url + "\n");

            // obtain a physical connection
            this._connection = DriverManager.getConnection(url, user, passwd);
            System.out.println("Done");

            //clean the CSV given files to produce alphanumeric tuples
            cleanCSV(new File("data/Connection.csv"), new int[]{0, 1});
            cleanCSV(new File("data/Edu_Det.csv"), new int[]{0});
            cleanCSV(new File("data/Message.csv"), new int[]{1, 2});
            cleanCSV(new File("data/Work_Ex.csv"), new int[]{0});
            cleanCSV(new File("data/USR.csv"), new int[]{0});
            cleanCSVSpacesOk(new File("data/USRProd.csv"), new int[]{3});

            // If not on campus, then run the SQL function below to mimic create_db.sh
            if (!onCampus) {
                importSQL(this, new File("sql/src/create_tables.sql"));
                importSQL(this, new File("sql/src/load_data.sql"));
                importSQL(this, new File("sql/src/create_index.sql"));
                importSQL(this, new File("sql/src/create_trigger.sql"));


            }

        } catch (Exception e) {
            System.err.println("Error - Unable to Connect to Database: " + e.getMessage());
            System.out.println("Make sure you started postgres on this machine");
            System.exit(-1);
        }//end catch
    }//end ProfNetwork

    /**
     * uses regular expressions to remove characters not passing a non-alphanumeric filter. Will keep spaces in this function. Produces a new file by the name of the given file appended with "Prod" at the end.
     * @param file The CSV file to read from
     * @param indeces What indexes (columns) to clean
     * @throws FileNotFoundException
     *
     */
    private static void cleanCSVSpacesOk(File file, int[] indeces) throws FileNotFoundException {

        ArrayList<String> lines = new ArrayList<>();
        Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] words = line.split(",");

            for (int i = 0; i < indeces.length; i++) {
                int indexToCheck = indeces[i];
                String checkThisWord = words[indexToCheck];
                if (!checkThisWord.matches("^[a-zA-Z0-9._ ']*$")) {
                    words[indexToCheck] = checkThisWord.replace(checkThisWord.replaceAll("[a-zA-Z0-9._ ']*", ""), "");
                }
            }

            String result = "";
            for (int i = 0; i < words.length; i++) {
                if (i == words.length - 1)
                    result += words[i];
                else
                    result += words[i] + ",";
            }
            lines.add(result);
        }
        scanner.close();
        FileWriter writer = null;
        try {
            writer = new FileWriter(file.getAbsoluteFile().getPath().substring(0, file.getAbsoluteFile().getPath().length() - 4) + "Prod.csv", false);
            for (String str : lines) {
                writer.write(str + System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * uses regular expressions to remove characters not passing a non-alphanumeric filter. Will not keep spaces in this function. Produces a new file by the name of the given file appended with "Prod" at the end.
     * @param file The CSV file to read from
     * @param indeces What indexes (columns) to clean
     * @throws FileNotFoundException
     *
     */
    private static void cleanCSV(File file, int[] indeces) throws FileNotFoundException {

        ArrayList<String> lines = new ArrayList<>();
        Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            String[] words = line.split(",");

            for (int i = 0; i < indeces.length; i++) {
                int indexToCheck = indeces[i];
                String checkThisWord = words[indexToCheck];
                if (!checkThisWord.matches("^[a-zA-Z0-9._']*$")) {
                    words[indexToCheck] = checkThisWord.replace(checkThisWord.replaceAll("[a-zA-Z0-9._']*", ""), "");
                }
            }

            String result = "";
            for (int i = 0; i < words.length; i++) {
                if (i == words.length - 1)
                    result += words[i];
                else
                    result += words[i] + ",";
            }
            lines.add(result);
        }
        scanner.close();
        FileWriter writer = null;
        try {
            writer = new FileWriter(file.getAbsoluteFile().getPath().substring(0, file.getAbsoluteFile().getPath().length() - 4) + "Prod.csv", false);
            for (String str : lines) {
                writer.write(str + System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A custom made SQL interpreter which tokenizes words. Removes comments and makes sure to only run when spots a semicolon.
     * @param profNetwork The SQL file
     * @param file The given file to import the SQL code from
     * @throws FileNotFoundException
     * @throws SQLException
     */
    private static void importSQL(ProfNetwork profNetwork, File file) throws FileNotFoundException, SQLException {
        Scanner scanner = new Scanner(file);

        Statement statement = profNetwork._connection.createStatement();

        String result = "";
        boolean isRun = true;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();

            if (line.contains("--"))
                line = line.substring(0, line.indexOf("--"));
            if (line.startsWith("--") || line.isEmpty())
                continue;

            result += " " + line;

            if (line.startsWith("$") && line.endsWith("$")) {
                isRun = !isRun;
            }
            if (!isRun) {
                continue;
            }


            if (!line.contains(";")) {
                continue;
            }


            result = result.substring(0, result.lastIndexOf(';'));

            statement.execute(result);
            statement.close();
            statement = profNetwork._connection.createStatement();
            result = "";
        }
        statement.close();
    }


    /**
     * Method to execute an update SQL statement.  Update SQL instructions
     * includes CREATE, INSERT, UPDATE, DELETE, and DROP.
     *
     * @param sql the input SQL string
     * @throws java.sql.SQLException when update failed
     */
    public void executeUpdate(String sql) throws SQLException {
        // creates a statement object
        Statement stmt = this._connection.createStatement();

        // issues the update instruction
        stmt.executeUpdate(sql);

        // close the instruction
        stmt.close();
    }//end executeUpdate

    /**
     * Method to execute an input query SQL instruction (i.e. SELECT).  This
     * method issues the query to the DBMS and outputs the results to
     * standard out.
     *
     * @param query the input query string
     * @return the number of rows returned
     * @throws java.sql.SQLException when failed to execute the query
     */
    public int executeQueryAndPrintResult(String query) throws SQLException {
        // creates a statement object
        Statement stmt = this._connection.createStatement();

        // issues the query instruction
        ResultSet rs = stmt.executeQuery(query);

        /*
         ** obtains the metadata object for the returned result set.  The metadata
         ** contains row and column info.
         */
        ResultSetMetaData rsmd = rs.getMetaData();
        int numCol = rsmd.getColumnCount();
        int rowCount = 0;

        // iterates through the result set and output them to standard out.
        boolean outputHeader = true;
        while (rs.next()) {
            if (outputHeader) {
                for (int i = 1; i <= numCol; i++) {
                    System.out.print(rsmd.getColumnName(i) + "\t");
                }
                System.out.println();
                outputHeader = false;
            }
            for (int i = 1; i <= numCol; ++i)
                System.out.print(rs.getString(i) + "\t");
            System.out.println();
            ++rowCount;
        }//end while
        stmt.close();
        return rowCount;
    }//end executeQuery

    /**
     * Method to execute an input query SQL instruction (i.e. SELECT).  This
     * method issues the query to the DBMS and returns the results as
     * a list of records. Each record in turn is a list of attribute values
     *
     * @param query the input query string
     * @return the query result as a list of records
     * @throws java.sql.SQLException when failed to execute the query
     */
    public List<List<String>> executeQueryAndReturnResult(String query) throws SQLException {
        // creates a statement object
        Statement stmt = this._connection.createStatement();

        // issues the query instruction
        ResultSet rs = stmt.executeQuery(query);

        /*
         ** obtains the metadata object for the returned result set.  The metadata
         ** contains row and column info.
         */
        ResultSetMetaData rsmd = rs.getMetaData();
        int numCol = rsmd.getColumnCount();
        int rowCount = 0;

        // iterates through the result set and saves the data returned by the query.
        boolean outputHeader = false;
        List<List<String>> result = new ArrayList<List<String>>();
        while (rs.next()) {
            List<String> record = new ArrayList<String>();
            for (int i = 1; i <= numCol; ++i)
                record.add(rs.getString(i));
            result.add(record);
        }//end while
        stmt.close();
        return result;
    }//end executeQueryAndReturnResult

    /**
     * Method to execute an input query SQL instruction (i.e. SELECT).  This
     * method issues the query to the DBMS and returns the number of results
     *
     * @param query the input query string
     * @return the number of rows returned
     * @throws java.sql.SQLException when failed to execute the query
     */
    public int executeQuery(String query) throws SQLException {
        // creates a statement object
        Statement stmt = this._connection.createStatement();

        // issues the query instruction
        ResultSet rs = stmt.executeQuery(query);

        int rowCount = 0;

        // iterates through the result set and count nuber of results.
        if (rs.next()) {
            rowCount++;
        }//end while
        stmt.close();
        return rowCount;
    }

    /**
     * Method to fetch the last value from sequence. This
     * method issues the query to the DBMS and returns the current
     * value of sequence used for autogenerated keys
     *
     * @param sequence name of the DB sequence
     * @return current value of a sequence
     * @throws java.sql.SQLException when failed to execute the query
     */
    public int getCurrSeqVal(String sequence) throws SQLException {
        Statement stmt = this._connection.createStatement();

        ResultSet rs = stmt.executeQuery(String.format("Select currval('%s')", sequence));
        if (rs.next())
            return rs.getInt(1);
        return -1;
    }

    /**
     * Method to close the physical connection if it is open.
     */
    public void cleanup() {
        try {
            if (this._connection != null) {
                this._connection.close();
            }//end if
        } catch (SQLException e) {
            // ignored.
        }//end try
    }//end cleanup

    /**
     * The main execution method
     *
     * @param args the command line arguments this inclues the <mysql|pgsql> <login file>
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println(
                    "Usage: " +
                            "java [-classpath <classpath>] " +
                            ProfNetwork.class.getName() +
                            " <dbname> <port> <user>");
            return;
        }//end if

        Greeting();
        ProfNetwork esql = null;
        try {
            // use postgres JDBC driver.

            Class.forName("org.postgresql.Driver").newInstance();
            // instantiate the Messenger object and creates a physical
            // connection.
            String dbname = args[0];
            String dbport = args[1];
            String user = args[2];
            String passwd = args[3];
            esql = new ProfNetwork(dbname, dbport, user, passwd);
            boolean keepon = true;
            while (keepon) {
                // These are sample SQL statements
                System.out.println("MAIN MENU");
                System.out.println("---------");
                System.out.println("1. Create user");
                System.out.println("2. Log in");
                System.out.println("9. < EXIT");
                String authorisedUser = null;
                switch (readChoice()) {
                    case 1:
                        CreateUser(esql);
                        break;
                    case 2:
                        authorisedUser = LogIn(esql);
                        break;
                    case 9:
                        keepon = false;
                        break;
                    default:
                        System.out.println("Unrecognized choice!");
                        break;
                }//end switch
                if (authorisedUser != null) {
                    boolean usermenu = true;
                    while (usermenu) {
                        System.out.println("MAIN MENU");
                        System.out.println("---------");
                        System.out.println("1. Goto Friend List");
                        System.out.println("2. View Account");
                        System.out.println("3. Write a new message");
                        System.out.println("4. Send Friend Request");
                        System.out.println("5. Search People");
                        System.out.println("6. Accept/Deny Friend Requests");
                        System.out.println("7. View Profile");
                        System.out.println("8. View Messages");
                        System.out.println(".........................");
                        System.out.println("9. Log out");
                        switch (readChoice()) {
                            case 1:
                                FriendList(esql, authorisedUser, authorisedUser);
                                break;
                            case 2:
                                ViewAccount(esql, authorisedUser);
                                break;
                            case 3:
                                NewMessage(esql, authorisedUser);
                                break;
                            case 4:
                                SendRequest(esql, authorisedUser);
                                break;
                            case 5:
                                SearchPeople(esql, authorisedUser);
                                break;
                            case 6:
                                AcceptDenyRequest(esql, authorisedUser);
                                break;
                            case 7:
                                GetProfileExactHELPER(esql, authorisedUser, authorisedUser);
                                break;
                            case 8:
                                ViewMessages(esql, authorisedUser);
                                break;
                            case 9:
                                usermenu = false;
                                break;
                            default:
                                System.out.println("Unrecognized choice!");
                                break;
                        }
                    }
                }
            }//end while
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            // make sure to cleanup the created table and close the connection.
            try {
                if (esql != null) {
                    System.out.print("Disconnecting from database...");
                    esql.cleanup();
                    System.out.println("Done\n\nBye !");
                }//end if
            } catch (Exception e) {
                // ignored.
            }//end try
        }//end try
    }//end main


    public static void Greeting() {
        System.out.println(
                "\n\n*******************************************************\n" +
                        "              User Interface      	               \n" +
                        "*******************************************************\n");
    }//end Greeting

    /*
     * Reads the users choice given from the keyboard. MODIFIED TO ALSO READ CHARACTERS IF CANNOT PARSE INTO AN INTEGER. IF CANNOT PARSE, TAKE FIRST CHARACTER AND CONVERT TO ASCII equivalent number.
     * @int
     **/
    public static int readChoice() {
        int input;
        boolean isSet = false;
        // returns only if a correct value is given.
        do {
            System.out.print("Please make your choice: ");
            String line = "";
            try { // read the integer, parse it and break.
                //try to get integer instetad
                line = in.readLine().trim();

                input = Integer.parseInt(line);
                break;
            } catch (NumberFormatException | IOException e) {

                input = line.charAt(0);
                input *= -1;
                break;
            }//end try
        } while (true);
        return input;
    }//end readChoice

    public static char readCharChoice() {
        char input;
        // returns only if a correct value is given.
        do {
            System.out.print("Please make your choice: ");
            try { // read the integer, parse it and break.
                input = in.readLine().charAt(0);
                break;
            } catch (Exception e) {
                System.out.println("Your input is invalid!");
                continue;
            }//end try
        } while (true);
        return input;
    }//end readChoice

    /*
     * Creates a new user with privided login, passowrd and phoneNum
     * An empty block and contact list would be generated and associated with a user
     **/
    public static void CreateUser(ProfNetwork esql) {
        try {
            System.out.println("\tEnter your first and last name (Ex: Jackson Johnson): ");
            String name = in.readLine();
            System.out.print("\tEnter user login: ");
            String login = in.readLine();
            System.out.print("\tEnter user password: ");
            String password = in.readLine();
            System.out.print("\tEnter user email: ");
            String email = in.readLine();
            System.out.print("\tEnter user birthdate in YYYY/MM/DD format (Ex: 1996/03/26): ");
            String birthDate = in.readLine();

            //Creating empty contact\block lists for a user
            String query = String.format("INSERT INTO USR (userId, password, email, name, dateOfBirth) VALUES ('%s','%s','%s','%s','%s')", login, password, email, name, birthDate);

            esql.executeUpdate(query);
            System.out.println("User successfully created!");
        } catch (Exception e) {
            System.out.println("User already exists");
            ;
        }
    }//end

    /*
     * Check log in credentials for an existing user
     * @return User login or null is the user does not exist
     **/
    public static String LogIn(ProfNetwork esql) {
        try {
            System.out.print("\tEnter user login: ");
            String login = in.readLine();
            System.out.print("\tEnter user password: ");
            String password = in.readLine();

            String query = String.format("SELECT * FROM USR WHERE userId = '%s' AND password = '%s'", login, password);
            int userNum = esql.executeQuery(query);
            if (userNum > 0)
                return login;
            return null;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
    }//end

    // Rest of the functions definition go in here

    /**
     * Friend list menu
     *
     * @param esql           sql file
     * @param authorisedUser requester
     * @param personId       requesting for this user's friendlist
     */
    private static void FriendList(ProfNetwork esql, String authorisedUser, String personId) {
        try {

            //Gets friend list that are accepted.
            String query1 =
                    String.format("-- Level 1\n" +
                            "SELECT C.connectionId\n" +
                            "FROM USR U, CONNECTION_USR C\n" +
                            "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                            "WHERE C.status = 'Accept' AND U.userId = C.userId AND C.status = 'Accept' AND (U.userId = '%s')\n" +
                            "UNION \n" +
                            "SELECT C2.userId\n" +
                            "FROM USR U2, CONNECTION_USR C2\n" +
                            "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                            "WHERE C2.status = 'Accept' AND U2.userId = C2.connectionId AND C2.status = 'Accept' AND (U2.userId = '%s');", personId, personId);


            boolean usermenu = true;
            int page = 0;
            while (usermenu) {
                List<List<String>> requestQueriesResult = esql.executeQueryAndReturnResult(query1);
                HashMap<Integer, String> getNumQueryMap = getFriendListPage(page, requestQueriesResult, authorisedUser, personId, true, true);
                HashMap<Integer, String> getNameMap = getFriendListPage(page, requestQueriesResult, authorisedUser, personId, false, false);

                if (getNumQueryMap == null && page == 0) {
                    System.out.println("No friends lists to display");
                    return;
                } else if (getNumQueryMap == null) {
                    page = 0;
                    continue;
                }

                System.out.println("........PAGE " + (page + 1) + "........");
                System.out.println("n. Next page");
                System.out.println("b. Go back a page");
                System.out.println("Q. Exit Friend List");

                int choice = readChoice();
                try {
                    int numChoice = Integer.parseInt("" + choice);
                    String query = getNumQueryMap.get(numChoice);

                    if (numChoice < 0) {
                        throw new NumberFormatException();
                    }

                    if (query == null) {
                        System.out.println("Invalid number.");
                        continue;
                    }


                    //esql.executeUpdate(query);
                    String userId = getNameMap.get(numChoice);

                    GetProfileExactHELPER(esql, authorisedUser, userId);
                    return;
                } catch (NumberFormatException e) {

                    choice *= -1;
                    if (choice == 'Q' || choice == 'q')
                        return;
                    else if (choice == 'N' || choice == 'n') {
                        page++;
                        continue;
                    } else if (choice == 'B' || choice == 'b') {
                        page--;
                        continue;
                    } else {
                        System.out.println("Invalid character.");
                        continue;
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //ONLY FOR AUTHORISED

    /**
     * View Profile menu
     *
     * @param esql           sql file
     * @param authorisedUser requester
     */
    private static void ViewAccount(ProfNetwork esql, String authorisedUser) {
        boolean usermenu = true;


        String query = String.format("SELECT * FROM USR WHERE userId = '%s'", authorisedUser);
        List<List<String>> userData;
        try {


            while (usermenu) {
                userData = esql.executeQueryAndReturnResult(query);

                final List<String> data = userData.get(0);
                System.out.println("PROFILE MENU");
                System.out.println("Hi, " + data.get(3).trim() + ". Nice to see you again.");
                System.out.println("Username: " + data.get(0).trim() + "");
                System.out.println("Email: " + data.get(2).trim() + "");
                System.out.println("Birth date: " + data.get(4).trim() + "");

                System.out.println("---------");
                System.out.println("1. Change password");
                System.out.println(".........................");
                System.out.println("9. Quit");
                switch (readChoice()) {
                    case 1:
                        ChangePassword(esql, authorisedUser);
                        break;
                    case 9:
                        usermenu = false;
                        break;
                    default:
                        System.out.println("Unrecognized choice!");
                        break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //ONLY FOR AUTHORISED

    /**
     * Changes a password
     *
     * @param esql           sql file
     * @param authorisedUser requester
     */
    private static void ChangePassword(ProfNetwork esql, String authorisedUser) {
        try {
            System.out.print("\tEnter your current password: ");
            String oldPassword = in.readLine();

            String query = String.format("SELECT * FROM USR WHERE userId = '%s' AND password = '%s';", authorisedUser, oldPassword);
            int userNum = esql.executeQuery(query);
            if (userNum == 0) {
                System.out.println("Incorrect password.");
                return;
            }

            System.out.print("\tEnter your new password: ");
            String password = in.readLine();

            System.out.print("\tConfirm your new password: ");
            String confirmPassword = in.readLine();

            if (!password.equals(confirmPassword)) {
                System.out.println("Your passwords do not match.");
                return;
            }
            String newPasswordQuery = String.format(
                    "UPDATE USR \n" +
                            "SET password = '%s'\n" +
                            "WHERE userId = '%s' AND password = '%s';",
                    password, authorisedUser, oldPassword);

            try {
                esql.executeUpdate(newPasswordQuery);
                System.out.println("Your password has been updated.");
            } catch (Exception e) {
                System.out.println("The password could not be updated...");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Menu to send a new message to a personId
     *
     * @param esql           sql file
     * @param authorisedUser sender
     */
    private static void NewMessage(ProfNetwork esql, String authorisedUser) {
        try {
            System.out.print("\tEnter a username: ");
            String personId = in.readLine();
            NewMessage(esql, authorisedUser, personId);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Sends a new message to a personId
     *
     * @param esql           sql file
     * @param authorisedUser sender
     * @param personId       receiver
     */
    private static void NewMessage(ProfNetwork esql, String authorisedUser, String personId) {
        try {

            String existsQuery = String.format("SELECT * FROM USR WHERE userId = '%s'", personId);
            int userNum = esql.executeQuery(existsQuery);
            if (userNum == 0) {
                System.out.println("User does not exist.");
                return;
            }

            if (personId.equals(authorisedUser)) {
                System.out.println("Can't send message to self.");
                return;
            }

            System.out.println("Enter your message: ");
            String contents = in.readLine();

            if (contents.length() > 500) {
                System.out.println("The message exceeded the max limit of 500.");
                return;
            }

            String messageConQuery = String.format(
                    "INSERT INTO MESSAGE (senderId, receiverId, contents, deleteStatus, status)\n" +
                            "VALUES ('%s', '%s', '%s', 0, \'Delivered\');", authorisedUser, personId, contents);


            esql.executeUpdate(messageConQuery);


        } catch (Exception e) {
            System.err.println("Your message could not be delivered.");
        }
    }


    //ONLY FOR AUTHORISED

    /**
     * Menu to add a friend
     *
     * @param esql           sql file
     * @param authorisedUser requester
     */
    private static void SendRequest(ProfNetwork esql, String authorisedUser) {
        try {
            System.out.print("\tEnter a username: ");
            String personId = in.readLine();
            SendRequest(esql, authorisedUser, personId);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    //ONLY FOR AUTHORISED

    /**
     * Sends friend request based on already knowing a personId
     *
     * @param esql           sql file
     * @param authorisedUser requester
     * @param personId       adding this person
     */
    private static void SendRequest(ProfNetwork esql, String authorisedUser, String personId) {
        try {

            String existsQuery = String.format("SELECT * FROM USR WHERE userId = '%s'", personId);
            int userNum = esql.executeQuery(existsQuery);
            if (userNum == 0) {
                System.out.println("User does not exist.");
                return;
            }

            String alreadyFriendsQuery = String.format("SELECT C.connectionId\n" +
                    "FROM USR U, CONNECTION_USR C\n" +
                    "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                    "WHERE U.userId = C.userId AND C.status = 'Accept' AND (U.userId = '¬%s¬')\n" +
                    "UNION \n" +
                    "SELECT C2.userId\n" +
                    "FROM USR U2, CONNECTION_USR C2\n" +
                    "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                    "WHERE U2.userId = C2.connectionId  AND C2.status = 'Accept' AND (U2.userId = '¬%s¬');", authorisedUser, authorisedUser).replace("¬", "");

            String possibleFriendIDSQuery = String.format(
                    "-- Level 1\n" +
                            "SELECT C.connectionId\n" +
                            "FROM USR U, CONNECTION_USR C\n" +
                            "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                            "WHERE U.userId = C.userId AND C.status = 'Accept' AND (U.userId = '¬%s¬')\n" +
                            "UNION \n" +
                            "SELECT C2.userId\n" +
                            "FROM USR U2, CONNECTION_USR C2\n" +
                            "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                            "WHERE U2.userId = C2.connectionId AND C2.status = 'Accept' AND (U2.userId = '¬%s¬')\n" +
                            "\n" +
                            "UNION\n" +
                            "\n" +
                            "-- Level 2\n" +
                            "SELECT C.connectionId\n" +
                            "FROM USR U, CONNECTION_USR C\n" +
                            "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                            "WHERE U.userId = C.userId AND C.status = 'Accept' AND (U.userId IN \n" +
                            "\t(\n" +
                            "\t\tSELECT CA.connectionId\n" +
                            "\t\tFROM USR UA, CONNECTION_USR CA\n" +
                            "\t\tINNER JOIN USR USR1A ON CA.connectionId = USR1A.userId \n" +
                            "\t\tWHERE UA.userId = CA.userId AND CA.status = 'Accept' AND (UA.userId = '¬%s¬')\n" +
                            "\t\tUNION \n" +
                            "\t\tSELECT C2A.userId\n" +
                            "\t\tFROM USR U2A, CONNECTION_USR C2A\n" +
                            "\t\tINNER JOIN USR USR2A ON C2A.userId = USR2A.userId \n" +
                            "\t\tWHERE U2A.userId = C2A.connectionId AND C2A.status = 'Accept' AND (U2A.userId = '¬%s¬')\n" +
                            "\t)\n" +
                            ")\n" +
                            "UNION \n" +
                            "SELECT C2.userId\n" +
                            "FROM USR U2, CONNECTION_USR C2\n" +
                            "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                            "WHERE U2.userId = C2.connectionId AND C2.status = 'Accept' AND (U2.userId IN \n" +
                            "\t(\n" +
                            "\t\tSELECT CA1.connectionId\n" +
                            "\t\tFROM USR UA1, CONNECTION_USR CA1\n" +
                            "\t\tINNER JOIN USR USR1A1 ON CA1.connectionId = USR1A1.userId \n" +
                            "\t\tWHERE UA1.userId = CA1.userId AND CA1.status = 'Accept' AND (UA1.userId = '¬%s¬')\n" +
                            "\t\tUNION \n" +
                            "\t\tSELECT C2A1.userId\n" +
                            "\t\tFROM USR U2A1, CONNECTION_USR C2A1\n" +
                            "\t\tINNER JOIN USR USR2A1 ON C2A1.userId = USR2A1.userId \n" +
                            "\t\tWHERE U2A1.userId = C2A1.connectionId AND C2A1.status = 'Accept' AND (U2A1.userId = '¬%s¬')\n" +
                            "\t)\n" +
                            ")\n" +
                            "\n" +
                            "UNION\n" +
                            "\n" +
                            "-- Level 3\n" +
                            "SELECT C.connectionId\n" +
                            "FROM USR U, CONNECTION_USR C\n" +
                            "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                            "WHERE U.userId = C.userId AND C.status = 'Accept' AND (U.userId IN \n" +
                            "\t(\n" +
                            "\tSELECT CB.connectionId\n" +
                            "\tFROM USR UB, CONNECTION_USR CB\n" +
                            "\tINNER JOIN USR USR1B ON CB.connectionId = USR1B.userId \n" +
                            "\tWHERE UB.userId = CB.userId AND CB.status = 'Accept' AND (UB.userId IN\n" +
                            "\t\t(\n" +
                            "\t\t\tSELECT CA.connectionId\n" +
                            "\t\t\tFROM USR UA, CONNECTION_USR CA\n" +
                            "\t\t\tINNER JOIN USR USR1A ON CA.connectionId = USR1A.userId \n" +
                            "\t\t\tWHERE UA.userId = CA.userId AND CA.status = 'Accept' AND (UA.userId = '¬%s¬')\n" +
                            "\t\t\tUNION \n" +
                            "\t\t\tSELECT C2A.userId\n" +
                            "\t\t\tFROM USR U2A, CONNECTION_USR C2A\n" +
                            "\t\t\tINNER JOIN USR USR2A ON C2A.userId = USR2A.userId \n" +
                            "\t\t\tWHERE U2A.userId = C2A.connectionId AND C2A.status = 'Accept' AND (U2A.userId = '¬%s¬')\n" +
                            "\t\t)\n" +
                            "\t)\n" +
                            "\tUNION \n" +
                            "\tSELECT C2B.userId\n" +
                            "\tFROM USR U2B, CONNECTION_USR C2B\n" +
                            "\tINNER JOIN USR USR2B ON C2B.userId = USR2B.userId \n" +
                            "\tWHERE U2B.userId = C2B.connectionId AND C2B.status = 'Accept' AND (U2B.userId IN \n" +
                            "\t\t(\n" +
                            "\t\t\tSELECT C3A.connectionId\n" +
                            "\t\t\tFROM USR U3A, CONNECTION_USR C3A\n" +
                            "\t\t\tINNER JOIN USR USR3A ON C3A.connectionId = USR3A.userId \n" +
                            "\t\t\tWHERE U3A.userId = C3A.userId AND C3A.status = 'Accept' AND (U3A.userId = '¬%s¬')\n" +
                            "\t\t\tUNION \n" +
                            "\t\t\tSELECT C4A.userId\n" +
                            "\t\t\tFROM USR U4A, CONNECTION_USR C4A\n" +
                            "\t\t\tINNER JOIN USR USR4A ON C4A.userId = USR4A.userId \n" +
                            "\t\t\tWHERE U4A.userId = C4A.connectionId AND C4A.status = 'Accept' AND (U4A.userId = '¬%s¬')\n" +
                            "\t\t)\n" +
                            "\t)\n" +
                            "\t)\n" +
                            ")\n" +
                            "UNION \n" +
                            "SELECT C2.userId\n" +
                            "FROM USR U2, CONNECTION_USR C2\n" +
                            "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                            "WHERE U2.userId = C2.connectionId AND C2.status = 'Accept' AND (U2.userId IN \n" +
                            "\t(\n" +
                            "\tSELECT C3B.connectionId\n" +
                            "\tFROM USR U3B, CONNECTION_USR C3B\n" +
                            "\tINNER JOIN USR USR3B ON C3B.connectionId = USR3B.userId \n" +
                            "\tWHERE U3B.userId = C3B.userId AND C3B.status = 'Accept' AND (U3B.userId IN \n" +
                            "\t\t(\n" +
                            "\t\t\tSELECT C5A.connectionId\n" +
                            "\t\t\tFROM USR U5A, CONNECTION_USR C5A\n" +
                            "\t\t\tINNER JOIN USR USR5A ON C5A.connectionId = USR5A.userId \n" +
                            "\t\t\tWHERE U5A.userId = C5A.userId AND C5A.status = 'Accept' AND (U5A.userId = '¬%s¬')\n" +
                            "\t\t\tUNION \n" +
                            "\t\t\tSELECT C6A.userId\n" +
                            "\t\t\tFROM USR U6A, CONNECTION_USR C6A\n" +
                            "\t\t\tINNER JOIN USR USR6A ON C6A.userId = USR6A.userId \n" +
                            "\t\t\tWHERE U6A.userId = C6A.connectionId AND C6A.status = 'Accept' AND (U6A.userId = '¬%s¬')\n" +
                            "\t\t)\n" +
                            "\t)\n" +
                            "\tUNION \n" +
                            "\tSELECT C4B.userId\n" +
                            "\tFROM USR U4B, CONNECTION_USR C4B\n" +
                            "\tINNER JOIN USR USR4B ON C4B.userId = USR4B.userId \n" +
                            "\tWHERE U4B.userId = C4B.connectionId AND C4B.status = 'Accept' AND (U4B.userId IN \n" +
                            "\t\t(\n" +
                            "\t\t\tSELECT C7A.connectionId\n" +
                            "\t\t\tFROM USR U7A, CONNECTION_USR C7A\n" +
                            "\t\t\tINNER JOIN USR USR7A ON C7A.connectionId = USR7A.userId \n" +
                            "\t\t\tWHERE U7A.userId = C7A.userId AND C7A.status = 'Accept' AND (U7A.userId = '¬%s¬')\n" +
                            "\t\t\tUNION \n" +
                            "\t\t\tSELECT C8A.userId\n" +
                            "\t\t\tFROM USR U8A, CONNECTION_USR C8A\n" +
                            "\t\t\tINNER JOIN USR USR8A ON C8A.userId = USR8A.userId \n" +
                            "\t\t\tWHERE U8A.userId = C8A.connectionId AND C8A.status = 'Accept' AND (U8A.userId = '¬%s¬')\n" +
                            "\t\t)\n" +
                            "\t)\n" +
                            "\t)\n" +
                            ");\n"
                    , authorisedUser, authorisedUser, authorisedUser, authorisedUser, authorisedUser, authorisedUser,
                    authorisedUser, authorisedUser, authorisedUser, authorisedUser, authorisedUser, authorisedUser,
                    authorisedUser, authorisedUser).replace("¬", "");

            String youAreRejectedResultQuery = String.format(
                    "SELECT C.connectionId\n" +
                            "FROM USR U, CONNECTION_USR C\n" +
                            "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                            "WHERE U.userId = C.userId AND C.status = 'Reject'AND (U.userId = '%s');", authorisedUser);

            String forgiveResultQuery = String.format(
                    "SELECT C2.userId\n" +
                            "FROM USR U2, CONNECTION_USR C2\n" +
                            "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                            "WHERE U2.userId = C2.connectionId AND C2.status = 'Reject' AND (U2.userId = '¬%s¬');", authorisedUser, authorisedUser).replace("¬", "");


            List<List<String>> alreadyFriendsResult = esql.executeQueryAndReturnResult(alreadyFriendsQuery);
            List<List<String>> usersResult = esql.executeQueryAndReturnResult(possibleFriendIDSQuery);
            List<List<String>> rejectedResult = esql.executeQueryAndReturnResult(youAreRejectedResultQuery);
            List<List<String>> forgiveResult = esql.executeQueryAndReturnResult(forgiveResultQuery);

            HashSet<String> alreadyFriendsSet = new HashSet<>();
            HashSet<String> possibleUserNamesSet = new HashSet<>();
            HashSet<String> rejectedSet = new HashSet<>();
            HashSet<String> forgiveSet = new HashSet<>();
            for (List<String> tuple : alreadyFriendsResult) {
                String userId = tuple.get(0).trim();
                alreadyFriendsSet.add(userId);
            }
            for (List<String> tuple : usersResult) {
                String userId = tuple.get(0).trim();
                possibleUserNamesSet.add(userId);
            }
            for (List<String> tuple : rejectedResult) {
                String userId = tuple.get(0).trim();
                rejectedSet.add(userId);
            }
            for (List<String> tuple : forgiveResult) {
                String userId = tuple.get(0).trim();
                forgiveSet.add(userId);
            }

            if (rejectedSet.contains(personId)) {
                System.out.println("You can't send any more friend connections to " + personId + " because you are rejected.");
                return;
            }

            if (alreadyFriendsSet.contains(personId)) {
                System.out.println("You are already the friend of " + personId + ".");
                return;
            }

            if (!possibleUserNamesSet.contains(personId) && alreadyFriendsSet.size() > 5) {
                System.out.println("Cannot send friend request because its beyond third level connection!");
                return;
            }

            String newFriendConnectReqQuery = String.format(
                    "INSERT INTO CONNECTION_USR(userId, connectionId, status)\n" +
                            "VALUES ('%s', '%s', 'Request');",
                    authorisedUser, personId);

            String forgiveQuery = String.format(
                    "UPDATE CONNECTION_USR \n" +
                            "SET status = 'Accept'\n" +
                            "WHERE userId = '%s' AND connectionId = '%s';",
                    personId, authorisedUser);

            try {
                if (forgiveResult.contains(personId)) {
                    esql.executeUpdate(forgiveQuery);
                    System.out.println("Accepted friend request!");
                } else {
                    esql.executeUpdate(newFriendConnectReqQuery);
                    System.out.println("Friend request sent!");
                }


                if (alreadyFriendsSet.size() < 4) {
                    System.out.println("Keep in mind: You have " + (5 - (alreadyFriendsSet.size() + 1)) + " friend connections remaining.");
                } else if (alreadyFriendsSet.size() == 4) {
                    System.out.println("Keep in mind: You used your last friend connection (if they accept).");
                }

            } catch (Exception e) {
                System.out.println("You've already sent a friend request.");
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    //ONLY FOR AUTHORISED
    private static void ViewMessages(ProfNetwork esql, String authorisedUser) {

        String requestQueries = String.format(
                "(\n" +
                        "\tSELECT M.sendTime, M.receiverId\n" +
                        "\tFROM MESSAGE  M\n" +
                        "\tWHERE M.senderId = '%s' AND (M.deleteStatus=0 OR M.deleteStatus=2)\n" +
                        "\tUNION\n" +
                        "\tSELECT M2.sendTime, M2.senderId\n" +
                        "\tFROM MESSAGE M2\n" +
                        "\tWHERE M2.receiverId = '%s' AND (M2.deleteStatus=0 OR M2.deleteStatus=1)\n" +
                        ")\n" +
                        "ORDER BY sendTime DESC;", authorisedUser, authorisedUser);

        try {

            boolean usermenu = true;
            int page = 0;
            while (usermenu) {
                List<List<String>> requestQueriesResult = esql.executeQueryAndReturnResult(requestQueries);
                HashMap<Integer, String> getNumQueryMap = getMessagePage(page, requestQueriesResult, authorisedUser, true, true);
                HashMap<Integer, String> getNameMap = getMessagePage(page, requestQueriesResult, authorisedUser, false, false);

                if (getNumQueryMap == null && page == 0) {
                    System.out.println("No messages with anyone to display");
                    return;
                } else if (getNumQueryMap == null) {
                    page = 0;
                    continue;
                }
                System.out.println("........PAGE " + (page + 1) + "........");
                System.out.println("n. Next page");
                System.out.println("b. Go back a page");
                System.out.println("Q. Exit Messages");

                int choice = readChoice();
                try {
                    int numChoice = Integer.parseInt("" + choice);
                    String query = getNumQueryMap.get(numChoice);
                    String otherPartyID = getNameMap.get(numChoice);

                    if (numChoice < 0) {
                        throw new NumberFormatException();
                    }

                    if (query == null) {
                        System.out.println("Invalid number.");
                        continue;
                    }


                    getMessageConversationMenu(esql, authorisedUser, otherPartyID, query);


                } catch (NumberFormatException e) {
                    choice *= -1;

                    if (choice == 'Q' || choice == 'q')
                        return;
                    else if (choice == 'N' || choice == 'n') {
                        page++;
                        continue;
                    } else if (choice == 'B' || choice == 'b') {
                        page--;
                        continue;
                    } else {
                        System.out.println("Invalid character.");
                        continue;
                    }
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    private static void getMessageConversationMenu(ProfNetwork esql, String authorisedUser, String otherPartyID, String messagesQuery) {


        List<List<String>> requestQueriesResult = null;
        try {


            boolean usermenu = true;
            int page = 0;
            while (usermenu) {


                requestQueriesResult = esql.executeQueryAndReturnResult(messagesQuery);
                HashMap<Integer, String> getNumQueryMap = getMessageConvoPage(page, requestQueriesResult, authorisedUser, otherPartyID);

                if (getNumQueryMap == null && page == 0) {
                    System.out.println("No messages to display");
                    return;
                } else if (getNumQueryMap == null) {
                    page = 0;
                    continue;
                }

                System.out.println("........PAGE " + (page + 1) + "........");
                System.out.println("n. Next page");
                System.out.println("b. Go back a page");
                System.out.println("Q. Exit Messages");

                int choice = readChoice();
                try {
                    int numChoice = Integer.parseInt("" + choice);
                    String deleteQuery = getNumQueryMap.get(numChoice);

                    if (numChoice < 0) {
                        throw new NumberFormatException();
                    }

                    if (deleteQuery == null) {
                        System.out.println("Invalid number.");
                        continue;
                    }


                    esql.executeUpdate(deleteQuery);
                    System.out.println("Deleted message successful!");

                } catch (NumberFormatException e) {

                    choice *= -1;

                    if (choice == 'Q' || choice == 'q')
                        return;
                    else if (choice == 'N' || choice == 'n') {
                        page++;
                        continue;
                    } else if (choice == 'B' || choice == 'b') {
                        page--;
                        continue;
                    } else {
                        System.out.println("Invalid character.");
                        continue;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private static HashMap<Integer, String> getMessageConvoPage(int page, List<List<String>> requestQueriesResult, String authorisedUser, String otherPartyID) {
        int entries = 0;
        final int RESULTS = 10; //RESULTS in 10 per page

        entries = page * RESULTS;

        HashMap<Integer, String> map = new HashMap<>();

        String deleteQuery =
                "UPDATE MESSAGE \n" +
                        "SET deleteStatus = %d\n" +
                        "WHERE msgId = %d;";


        if (entries < 0 || entries >= requestQueriesResult.size())
            return null;


        System.out.println("MESSAGES WITH " + otherPartyID);
        System.out.println("---------");

        for (int i = entries; i < requestQueriesResult.size(); i++) {
            List<String> tuple = requestQueriesResult.get(i);
            String timestamp = tuple.get(0).trim();
            String senderParty = tuple.get(1).trim();
            String receiverParty = tuple.get(2).trim();
            String message = tuple.get(3).trim();
            String msgId = tuple.get(4).trim();
            String deleteStatus = tuple.get(5).trim();
            int newStatus = 0;
            System.out.println(
                    "[" + (entries++) + "] Delete Message Below\n" +
                            "[" + timestamp + "] " + senderParty + ": " + message);

            if (senderParty.equals(authorisedUser)) {

                if (deleteStatus.equals("0"))
                    newStatus = 1;
                else if (deleteStatus.equals("2"))
                    newStatus = 3;

                map.put(entries - 1, String.format(deleteQuery, newStatus, Integer.parseInt(msgId)));
            } else {

                if (deleteStatus.equals("0"))
                    newStatus = 2;
                else if (deleteStatus.equals("1"))
                    newStatus = 3;
                map.put(entries - 1, String.format(deleteQuery, newStatus, Integer.parseInt(msgId)));
            }

            if (entries % RESULTS == 0)
                break;
        }
        return map;
    }

    /**
     * Gets a list of friend requests being sent to this authorisedUser
     *
     * @param page                 0-based page to search
     * @param requestQueriesResult Collection of all personIds
     * @param authorisedUser       Requester
     * @param isQuery              if a query is being requested instead of personId
     * @return if isQuery is true, returns a hashmap of Key=Integer and String=SQL code to execute, if false, returns personId.. If page is invalid, returns null.
     */
    private static HashMap<Integer, String> getMessagePage(int page, List<List<String>> requestQueriesResult, String authorisedUser, boolean isQuery, boolean shouldPrint) {
        int entries = 0;
        final int RESULTS = 10; //RESULTS in 10 per page

        entries = page * RESULTS;

        HashMap<Integer, String> map = new HashMap<>();

        String query1 =
                "(\n" +
                        "\tSELECT M.sendTime, M.senderId, M.receiverId, M.contents, M.msgId, M.deleteStatus\n" +
                        "\tFROM MESSAGE M\n" +
                        "\tWHERE M.senderId = '%s' AND M.receiverId = '%s' AND (M.deleteStatus=0 OR M.deleteStatus=2)\n" +
                        "\tUNION\n" +
                        "\tSELECT M2.sendTime, M2.senderId, M2.receiverId, M2.contents, M2.msgId, M2.deleteStatus\n" +
                        "\tFROM MESSAGE M2\n" +
                        "\tWHERE M2.receiverId = '%s' AND M2.senderId = '%s' AND (M2.deleteStatus=0 OR M2.deleteStatus=1)\n" +
                        ")\n" +
                        "ORDER BY sendTime DESC;\n";


        if (entries < 0 || entries >= requestQueriesResult.size())
            return null;

        if (shouldPrint) {
            System.out.println("MESSAGES MENU");
            System.out.println("---------");
        }

        ArrayList<String> alreadyIDList = new ArrayList<>();

        for (int i = entries; i < requestQueriesResult.size(); i++) {
            List<String> tuple = requestQueriesResult.get(i);
            String otherPartyID = tuple.get(1).trim();

            if (alreadyIDList.contains(otherPartyID))
                continue;
            alreadyIDList.add(otherPartyID);

            String s = "[" + (entries++) + "] View \t\t" + otherPartyID;
            if (shouldPrint) {
                System.out.println(s);
            }
            if (isQuery) {
                map.put(entries - 1, String.format(query1, authorisedUser, otherPartyID, authorisedUser, otherPartyID));
            } else {
                map.put(entries - 1, otherPartyID);
            }

            if (entries % RESULTS == 0)
                break;
        }
        return map;
    }

    //ONLY FOR AUTHORISED

    /**
     * Accepts or Denies friend requests menu
     *
     * @param esql           sql file
     * @param authorisedUser requester
     */
    private static void AcceptDenyRequest(ProfNetwork esql, String authorisedUser) {

        try {

            String requestQueries = String.format("SELECT C.userId, U.name\n" +
                    "FROM CONNECTION_USR C\n" +
                    "INNER JOIN USR U ON U.userId = C.userId\n" +
                    "WHERE C.status = 'Request' AND C.connectionId = '%s';", authorisedUser);


            boolean usermenu = true;
            int page = 0;
            while (usermenu) {
                List<List<String>> requestQueriesResult = esql.executeQueryAndReturnResult(requestQueries);
                HashMap<Integer, String> getNumQueryMap = getFriendConnectionRequestsPage(page, requestQueriesResult, authorisedUser, true, true);
                HashMap<Integer, String> getNameMap = getFriendConnectionRequestsPage(page, requestQueriesResult, authorisedUser, false, false);

                if (getNumQueryMap == null && page == 0) {
                    System.out.println("No friend connections to display");
                    return;
                } else if (getNumQueryMap == null) {
                    page = 0;
                    continue;
                }

                System.out.println("........PAGE " + (page + 1) + "........");
                System.out.println("n. Next page");
                System.out.println("b. Go back a page");
                System.out.println("Q. Exit Friend Connection Requests");

                int choice = readChoice();
                try {
                    int numChoice = Integer.parseInt("" + choice);
                    String query = getNumQueryMap.get(numChoice);

                    if (numChoice < 0) {
                        throw new NumberFormatException();
                    }
                    if (query == null) {
                        System.out.println("Invalid number.");
                        continue;
                    }


                    esql.executeUpdate(query);
                    if (numChoice % 2 == 0) {
                        System.out.println("Added " + getNameMap.get(numChoice) + " to your connections!");

                    } else {
                        System.out.println("Rejected " + getNameMap.get(numChoice) + " from your connections!");

                    }


                } catch (NumberFormatException e) {

                    choice *= -1;
                    if (choice == 'Q' || choice == 'q')
                        return;
                    else if (choice == 'N' || choice == 'n') {
                        page++;
                        continue;
                    } else if (choice == 'B' || choice == 'b') {
                        page--;
                        continue;
                    } else {
                        System.out.println("Invalid character.");
                        continue;
                    }
                }
            }


        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    /**
     * Gets the friend list page of a personId
     *
     * @param page                 0-based
     * @param requestQueriesResult Collection of person ids
     * @param authorisedUser       Requester
     * @param personId             Searching for this PERSON ID
     * @param isQuery              if a query is being requested instead of personId
     * @return if isQuery is true, returns a hashmap of Key=Integer and String=SQL code to execute, if false, returns personId.. If page is invalid, returns null.
     */
    private static HashMap<Integer, String> getFriendListPage(int page, List<List<String>> requestQueriesResult, String authorisedUser, String personId, boolean isQuery, boolean shouldPrint) {

        int entries = 0;
        final int RESULTS = 10; //RESULTS in 10 per page

        entries = page * RESULTS;

        HashMap<Integer, String> map = new HashMap<>();

        String query1 = String.format(
                "-- Level 1\n" +
                        "SELECT C.connectionId\n" +
                        "FROM USR U, CONNECTION_USR C\n" +
                        "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                        "WHERE U.userId = C.userId AND C.status = 'Accept' AND (U.userId = '%s')\n" +
                        "UNION \n" +
                        "SELECT C2.userId\n" +
                        "FROM USR U2, CONNECTION_USR C2\n" +
                        "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                        "WHERE U2.userId = C2.connectionId AND C2.status = 'Accept' AND (U2.userId = '%s');", personId, personId);

        if (entries < 0 || entries >= requestQueriesResult.size())
            return null;

        if (shouldPrint) {
            System.out.println(authorisedUser + "'s FRIEND LIST");
            System.out.println("---------");
        }

        for (int i = entries; i < requestQueriesResult.size(); i++) {
            List<String> tuple = requestQueriesResult.get(i);
            String userId = tuple.get(0).trim();

            String s = "[" + (entries++) + "] View\t\t" + userId;
            if (shouldPrint)
                System.out.println(s);

            if (isQuery) {
                map.put(entries - 1, String.format(query1, userId, userId));
            } else {
                map.put(entries - 1, userId);
            }

            if (entries % RESULTS == 0)
                break;
        }


        return map;
    }

    //ONLY FOR AUTHORISED

    /**
     * Gets a list of friend requests being sent to this authorisedUser
     *
     * @param page                 0-based page to search
     * @param requestQueriesResult Collection of all personIds
     * @param authorisedUser       Requester
     * @param isQuery              if a query is being requested instead of personId
     * @return if isQuery is true, returns a hashmap of Key=Integer and String=SQL code to execute, if false, returns personId.. If page is invalid, returns null.
     */
    private static HashMap<Integer, String> getFriendConnectionRequestsPage(int page, List<List<String>> requestQueriesResult, String authorisedUser, boolean isQuery, boolean shouldPrint) {
        int entries = 0;
        final int RESULTS = 20; //RESULTS in 10 per page

        entries = page * RESULTS;

        HashMap<Integer, String> map = new HashMap<>();

        String query1 = "UPDATE CONNECTION_USR \n" +
                "SET status = 'Accept'\n" +
                "WHERE userId = '%s' AND connectionId = '%s';";

        String query2 = "UPDATE CONNECTION_USR \n" +
                "SET status = 'Reject'\n" +
                "WHERE userId = '%s' AND connectionId = '%s';";

        if (entries < 0 || entries >= requestQueriesResult.size())
            return null;

        if (shouldPrint) {
            System.out.println("FRIEND CONNECTION REQUESTS");
            System.out.println("---------");
        }

        for (int i = entries; i < requestQueriesResult.size(); i++) {
            List<String> tuple = requestQueriesResult.get(i);
            String userId = tuple.get(0).trim();
            String name = tuple.get(1).trim();

            String s = "[" + (entries++) + "] Accept\t[" + (entries++) + "] Reject\t\t" + userId + "\t" + name;
            if (shouldPrint)
                System.out.println(s);

            if (isQuery) {
                map.put(entries - 2, String.format(query1, userId, authorisedUser));
                map.put(entries - 1, String.format(query2, userId, authorisedUser));
            } else {
                map.put(entries - 2, userId);
                map.put(entries - 1, userId);
            }

            if (entries % RESULTS == 0)
                break;
        }
        return map;
    }

    /**
     * Gets an exact profile based on personId if it exists
     *
     * @param esql           SQL file
     * @param authorisedUser Requester
     * @param personId       Explicit person ID
     */
    private static void GetProfileExactHELPER(ProfNetwork esql, String authorisedUser, String personId) {
        try {

            String areConnected = String.format(
                    "SELECT C.userId\n" +
                            "FROM CONNECTION_USR C\n" +
                            "WHERE C.status = 'Accept' AND ((C.userId = '%s' AND C.connectionId = '%s') OR (C.userId = '%s' AND C.connectionId = '%s'));", authorisedUser, personId, personId, authorisedUser);

            String userData = String.format(
                    "SELECT U.userId, U.name, U.dateOfBirth\n" +
                            "FROM USR U\n" +
                            "WHERE U.userId = '%s';"
                    , personId);

            String workExperience = String.format(
                    "SELECT U.userId, U.name, W.company, W.role, W.location, W.startDate, W.endDate\n" +
                            "FROM USR U, WORK_EXPR W\n" +
                            "WHERE (U.userId = W.userId) AND U.userId IN \n" +
                            "(\n" +
                            "\tSELECT USR2.userId\n" +
                            "\tFROM USR USR2\n" +
                            "\tWHERE USR2.userId = '%s'\n" +
                            ");"
                    , personId).replace("¬", "%");

            String educationDetails = String.format(
                    "SELECT U.userId, U.name, E.instituitionName, E.major, E.degree, E.startdate, E.enddate\n" +
                            "FROM USR U, EDUCATIONAL_DETAILS E\n" +
                            "WHERE (U.userId = E.userId) AND U.userId IN \n" +
                            "(\n" +
                            "\tSELECT USR2.userId\n" +
                            "\tFROM USR USR2\n" +
                            "\tWHERE USR2.userId = '%s'\n" +
                            ");"
                    , personId).replace("¬", "%");
            String friendsList = String.format(
                    "SELECT U.userId, U.name, C.connectionId, USR1.name \n" +
                            "FROM USR U, CONNECTION_USR C\n" +
                            "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                            "WHERE C.status = 'Accept' AND U.userId = C.userId AND (U.userId = '%s')\n" +
                            "UNION \n" +
                            "SELECT U2.userId, U2.name, C2.userId, USR2.name \n" +
                            "FROM USR U2, CONNECTION_USR C2\n" +
                            "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                            "WHERE C2.status = 'Accept' AND U2.userId = C2.connectionId AND (U2.userId = '%s');"
                    , personId, personId).replace("¬", "%");

            boolean usermenu = true;
            int page = 0;

            while (usermenu) {

                boolean isConnected = esql.executeQuery(areConnected) >= 1;

                List<List<String>> userDataResult = esql.executeQueryAndReturnResult(userData);
                List<List<String>> workExperienceResult = esql.executeQueryAndReturnResult(workExperience);
                List<List<String>> educationDetailsResult = esql.executeQueryAndReturnResult(educationDetails);
                List<List<String>> friendsListResult = esql.executeQueryAndReturnResult(friendsList);

                List<List<String>> userDataMap = new ArrayList<>();
                List<List<String>> workExperienceMap = new ArrayList<>();
                List<List<String>> educationDetailsMap = new ArrayList<>();
                List<List<String>> friendsListMap = new ArrayList<>();

                for (List<String> tuple : userDataResult) {
                    ArrayList<String> list = new ArrayList<>(tuple);
                    if (!list.isEmpty())
                        list.remove(0);
                    userDataMap.add(list);
                }


                for (List<String> tuple : workExperienceResult) {
                    ArrayList<String> list = new ArrayList<>(tuple);
                    if (!list.isEmpty())
                        list.remove(0);
                    if (!list.isEmpty())
                        list.remove(0);
                    workExperienceMap.add(list);
                }

                for (List<String> tuple : educationDetailsResult) {
                    ArrayList<String> list = new ArrayList<>(tuple);
                    if (!list.isEmpty())
                        list.remove(0);
                    if (!list.isEmpty())
                        list.remove(0);
                    educationDetailsMap.add(list);
                }


                for (List<String> tuple : friendsListResult) {
                    ArrayList<String> list = new ArrayList<>(tuple);
                    if (!list.isEmpty())
                        list.remove(0);
                    if (!list.isEmpty())
                        list.remove(0);
                    friendsListMap.add(list);
                }


                System.out.println(userDataMap.get(0).get(0).trim() + "'s PROFILE");
                System.out.println("Username: " + personId);

                if (isConnected)
                    System.out.println("Date of Birth: " + userDataMap.get(0).get(1).trim());
                System.out.println("Full name: " + userDataMap.get(0).get(0).trim());
                System.out.println("Friends?: " + (isConnected ? "✔" : "✘"));
                System.out.println();

                for (int i = 0; i < educationDetailsMap.size(); i++) {

                    List<String> row = educationDetailsMap.get(i);

                    if (i == 0) {
                        System.out.println("-Education Details-");
                        System.out.println("Institution\tMajor\tDegree\tStart Date\tEnd Date");
                    }

                    for (String word : row) {
                        System.out.print(word + "\t");
                    }
                    System.out.println();
                }
                System.out.println();
                for (int i = 0; i < workExperienceMap.size(); i++) {

                    List<String> row = workExperienceMap.get(i);

                    if (i == 0) {
                        System.out.println("-Work Details-");
                        System.out.println("Company\tRole\tLocation\tStart Date\tEnd Date");
                    }

                    for (String word : row) {
                        System.out.print(word + "\t");
                    }
                    System.out.println();
                }

                System.out.println("........PAGE " + (page + 1) + "........");
                System.out.println("V. View friends");
                System.out.println("M. Message");
                if (!isConnected && isWithinConnection(esql, authorisedUser, personId))
                    System.out.println("F. Send Friend Connection Request");
                System.out.println("Q. Quit");


                int choice = readChoice();
                try {
                    int numChoice = Integer.parseInt("" + choice);

                    if (numChoice < 0) {
                        throw new NumberFormatException();
                    }

                    System.out.println("Number not allowed.");
                    continue;
                } catch (NumberFormatException e) {

                    choice *= -1;

                    if (choice == 'V' || choice == 'v') {
                        FriendList(esql, authorisedUser, personId);
                    } else if (choice == 'M' || choice == 'm') {
                        NewMessage(esql, authorisedUser, personId);
                        continue;
                    } else if ((choice == 'F' || choice == 'f') && (!isConnected && isWithinConnection(esql, authorisedUser, personId))) {
                        SendRequest(esql, authorisedUser, personId);
                        continue;
                    } else if (choice == 'Q' || choice == 'q') {
                        return;

                    } else {
                        System.out.println("Invalid character.");
                        continue;
                    }
                }
            }


        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Searches for people
     *
     * @param esql           SQL file
     * @param authorisedUser Requester
     * @param personId       Looking for?
     */
    private static void GetProfileClosestMatchHELPER(ProfNetwork esql, String authorisedUser, String personId) {
        try {

            String allUserIds = String.format("SELECT U.userId, U.name\n" +
                    "FROM USR U\n" +
                    "WHERE lower(U.userId) LIKE '¬%s¬' OR lower(U.name) LIKE '¬%s¬';", personId, personId).replace("¬", "%");

            List<List<String>> usersResult = esql.executeQueryAndReturnResult(allUserIds);


            getSortedSearchPeople(usersResult, personId.toCharArray());
            SelectPersonMenu(esql, usersResult, authorisedUser, personId);


        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Gets the page menu for selecting a  person id
     *
     * @param esql           SQL file
     * @param list           The collection of person ids
     * @param authorisedUser Requester
     * @param personId       Looking for this ID
     */
    private static void SelectPersonMenu(ProfNetwork esql, List<List<String>> list, String authorisedUser, String personId) {
        boolean usermenu = true;
        int page = 0;
        while (usermenu) {

            HashMap<Integer, String> getNameMap = getSearchListPage(page, list, authorisedUser, personId);

            if (getNameMap == null && page == 0) {
                System.out.println("No people found.");
            } else if (getNameMap == null) {
                page = 0;
                continue;
            }

            System.out.println("........PAGE " + (page + 1) + "........");
            System.out.println("n. Next page");
            System.out.println("b. Go back a page");
            System.out.println("Q. Exit Friend Connection Requests");

            int choice = readChoice();
            try {
                int numChoice = Integer.parseInt("" + choice);

                if (numChoice < 0) {
                    throw new NumberFormatException();
                }

                String userId = getNameMap.get(numChoice);
                GetProfileExactHELPER(esql, authorisedUser, userId);
                return;
            } catch (NumberFormatException e) {

                choice *= -1;

                if (choice == 'Q' || choice == 'q')
                    return;
                else if (choice == 'N' || choice == 'n') {
                    page++;
                    continue;
                } else if (choice == 'B' || choice == 'b') {
                    page--;
                    continue;
                } else {
                    System.out.println("Invalid character.");
                    continue;
                }
            }
        }
    }

    /**
     * Gets a page entry of a collection
     *
     * @param page                 page requested 0-based
     * @param requestQueriesResult Collection of person id
     * @param authorisedUser       Requester
     * @param personId             Searching for this person
     * @return A map of Key=integer and Value=exact person ID. If page is invalid, returns null
     */
    private static HashMap<Integer, String> getSearchListPage(int page, List<List<String>> requestQueriesResult, String authorisedUser, String personId) {

        int entries = 0;
        final int RESULTS = 10; //RESULTS in 10 per page

        entries = page * RESULTS;

        HashMap<Integer, String> map = new HashMap<>();

        if (entries < 0 || entries >= requestQueriesResult.size()) {
            return null;
        }

        System.out.println(personId + " SEARCH");
        System.out.println("---------");

        for (int i = entries; i < requestQueriesResult.size(); i++) {
            List<String> tuple = requestQueriesResult.get(i);
            String userId = tuple.get(0).trim();
            String name = tuple.get(1).trim();


            System.out.println("[" + (entries++) + "] View\t\t" + userId + "\t" + name);

            map.put(entries - 1, userId);


            if (entries % RESULTS == 0)
                break;
        }


        return map;
    }

    /**
     * Searches for people by asking for a person's ID as input.
     *
     * @param esql           The SQL class
     * @param authorisedUser The person requesting this info
     */
    private static void SearchPeople(ProfNetwork esql, String authorisedUser) {
        try {
            System.out.print("\tEnter a person's name OR userId: ");
            String personId = in.readLine();
            String existsQuery = String.format("SELECT * FROM USR WHERE userId = '%s';", personId);
            int userNum = esql.executeQuery(existsQuery);
            if (userNum >= 1) {
                GetProfileExactHELPER(esql, authorisedUser, personId);
                //System.out.println("User does not exist.");
                return;
            }
            GetProfileClosestMatchHELPER(esql, authorisedUser, personId);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Sorts all results based on  their Levenshtein distance
     *
     * @param list     The collection of people
     * @param personId Person ID we are comparing to
     */
    private static void getSortedSearchPeople(List<List<String>> list, char[] personId) {

        Collections.sort(list, (a, b) ->

                (dist(a.get(0).trim().toCharArray(), personId) + dist(a.get(1).trim().toCharArray(), personId)) - (dist(b.get(0).trim().toCharArray(), personId) + dist(b.get(1).trim().toCharArray(), personId))
        );
    }


    /**
     * Levenshtein distance between two strings
     *
     * @param s1 String 1
     * @param s2 String 2
     * @return the cost between two strings
     */
    public static int dist(char[] s1, char[] s2) {

        // memoize only previous line of distance matrix
        int[] prev = new int[s2.length + 1];

        for (int j = 0; j < s2.length + 1; j++) {
            prev[j] = j;
        }

        for (int i = 1; i < s1.length + 1; i++) {

            // calculate current line of distance matrix
            int[] curr = new int[s2.length + 1];
            curr[0] = i;

            for (int j = 1; j < s2.length + 1; j++) {
                int d1 = prev[j] + 1;
                int d2 = curr[j - 1] + 1;
                int d3 = prev[j - 1];
                if (s1[i - 1] != s2[j - 1]) {
                    d3 += 1;
                }
                curr[j] = Math.min(Math.min(d1, d2), d3);
            }

            // define current line of distance matrix as previous
            prev = curr;
        }
        return prev[s2.length];
    }

    private static boolean isWithinConnection(ProfNetwork esql, String authorisedUser, String personId) {
        String possibleFriendIDSQuery = String.format(
                "-- Level 1\n" +
                        "SELECT C.connectionId\n" +
                        "FROM USR U, CONNECTION_USR C\n" +
                        "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                        "WHERE U.userId = C.userId AND C.status = 'Accept' AND (U.userId = '¬%s¬')\n" +
                        "UNION \n" +
                        "SELECT C2.userId\n" +
                        "FROM USR U2, CONNECTION_USR C2\n" +
                        "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                        "WHERE U2.userId = C2.connectionId AND C2.status = 'Accept' AND (U2.userId = '¬%s¬')\n" +
                        "\n" +
                        "UNION\n" +
                        "\n" +
                        "-- Level 2\n" +
                        "SELECT C.connectionId\n" +
                        "FROM USR U, CONNECTION_USR C\n" +
                        "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                        "WHERE U.userId = C.userId AND C.status = 'Accept' AND (U.userId IN \n" +
                        "\t(\n" +
                        "\t\tSELECT CA.connectionId\n" +
                        "\t\tFROM USR UA, CONNECTION_USR CA\n" +
                        "\t\tINNER JOIN USR USR1A ON CA.connectionId = USR1A.userId \n" +
                        "\t\tWHERE UA.userId = CA.userId AND CA.status = 'Accept' AND (UA.userId = '¬%s¬')\n" +
                        "\t\tUNION \n" +
                        "\t\tSELECT C2A.userId\n" +
                        "\t\tFROM USR U2A, CONNECTION_USR C2A\n" +
                        "\t\tINNER JOIN USR USR2A ON C2A.userId = USR2A.userId \n" +
                        "\t\tWHERE U2A.userId = C2A.connectionId AND C2A.status = 'Accept' AND (U2A.userId = '¬%s¬')\n" +
                        "\t)\n" +
                        ")\n" +
                        "UNION \n" +
                        "SELECT C2.userId\n" +
                        "FROM USR U2, CONNECTION_USR C2\n" +
                        "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                        "WHERE U2.userId = C2.connectionId AND C2.status = 'Accept' AND (U2.userId IN \n" +
                        "\t(\n" +
                        "\t\tSELECT CA1.connectionId\n" +
                        "\t\tFROM USR UA1, CONNECTION_USR CA1\n" +
                        "\t\tINNER JOIN USR USR1A1 ON CA1.connectionId = USR1A1.userId \n" +
                        "\t\tWHERE UA1.userId = CA1.userId AND CA1.status = 'Accept' AND (UA1.userId = '¬%s¬')\n" +
                        "\t\tUNION \n" +
                        "\t\tSELECT C2A1.userId\n" +
                        "\t\tFROM USR U2A1, CONNECTION_USR C2A1\n" +
                        "\t\tINNER JOIN USR USR2A1 ON C2A1.userId = USR2A1.userId \n" +
                        "\t\tWHERE U2A1.userId = C2A1.connectionId AND C2A1.status = 'Accept' AND (U2A1.userId = '¬%s¬')\n" +
                        "\t)\n" +
                        ")\n" +
                        "\n" +
                        "UNION\n" +
                        "\n" +
                        "-- Level 3\n" +
                        "SELECT C.connectionId\n" +
                        "FROM USR U, CONNECTION_USR C\n" +
                        "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                        "WHERE U.userId = C.userId AND C.status = 'Accept' AND (U.userId IN \n" +
                        "\t(\n" +
                        "\tSELECT CB.connectionId\n" +
                        "\tFROM USR UB, CONNECTION_USR CB\n" +
                        "\tINNER JOIN USR USR1B ON CB.connectionId = USR1B.userId \n" +
                        "\tWHERE UB.userId = CB.userId AND CB.status = 'Accept' AND (UB.userId IN\n" +
                        "\t\t(\n" +
                        "\t\t\tSELECT CA.connectionId\n" +
                        "\t\t\tFROM USR UA, CONNECTION_USR CA\n" +
                        "\t\t\tINNER JOIN USR USR1A ON CA.connectionId = USR1A.userId \n" +
                        "\t\t\tWHERE UA.userId = CA.userId AND CA.status = 'Accept' AND (UA.userId = '¬%s¬')\n" +
                        "\t\t\tUNION \n" +
                        "\t\t\tSELECT C2A.userId\n" +
                        "\t\t\tFROM USR U2A, CONNECTION_USR C2A\n" +
                        "\t\t\tINNER JOIN USR USR2A ON C2A.userId = USR2A.userId \n" +
                        "\t\t\tWHERE U2A.userId = C2A.connectionId AND C2A.status = 'Accept' AND (U2A.userId = '¬%s¬')\n" +
                        "\t\t)\n" +
                        "\t)\n" +
                        "\tUNION \n" +
                        "\tSELECT C2B.userId\n" +
                        "\tFROM USR U2B, CONNECTION_USR C2B\n" +
                        "\tINNER JOIN USR USR2B ON C2B.userId = USR2B.userId \n" +
                        "\tWHERE U2B.userId = C2B.connectionId AND C2B.status = 'Accept' AND (U2B.userId IN \n" +
                        "\t\t(\n" +
                        "\t\t\tSELECT C3A.connectionId\n" +
                        "\t\t\tFROM USR U3A, CONNECTION_USR C3A\n" +
                        "\t\t\tINNER JOIN USR USR3A ON C3A.connectionId = USR3A.userId \n" +
                        "\t\t\tWHERE U3A.userId = C3A.userId AND C3A.status = 'Accept' AND (U3A.userId = '¬%s¬')\n" +
                        "\t\t\tUNION \n" +
                        "\t\t\tSELECT C4A.userId\n" +
                        "\t\t\tFROM USR U4A, CONNECTION_USR C4A\n" +
                        "\t\t\tINNER JOIN USR USR4A ON C4A.userId = USR4A.userId \n" +
                        "\t\t\tWHERE U4A.userId = C4A.connectionId AND C4A.status = 'Accept' AND (U4A.userId = '¬%s¬')\n" +
                        "\t\t)\n" +
                        "\t)\n" +
                        "\t)\n" +
                        ")\n" +
                        "UNION \n" +
                        "SELECT C2.userId\n" +
                        "FROM USR U2, CONNECTION_USR C2\n" +
                        "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                        "WHERE U2.userId = C2.connectionId AND C2.status = 'Accept' AND (U2.userId IN \n" +
                        "\t(\n" +
                        "\tSELECT C3B.connectionId\n" +
                        "\tFROM USR U3B, CONNECTION_USR C3B\n" +
                        "\tINNER JOIN USR USR3B ON C3B.connectionId = USR3B.userId \n" +
                        "\tWHERE U3B.userId = C3B.userId AND C3B.status = 'Accept' AND (U3B.userId IN \n" +
                        "\t\t(\n" +
                        "\t\t\tSELECT C5A.connectionId\n" +
                        "\t\t\tFROM USR U5A, CONNECTION_USR C5A\n" +
                        "\t\t\tINNER JOIN USR USR5A ON C5A.connectionId = USR5A.userId \n" +
                        "\t\t\tWHERE U5A.userId = C5A.userId AND C5A.status = 'Accept' AND (U5A.userId = '¬%s¬')\n" +
                        "\t\t\tUNION \n" +
                        "\t\t\tSELECT C6A.userId\n" +
                        "\t\t\tFROM USR U6A, CONNECTION_USR C6A\n" +
                        "\t\t\tINNER JOIN USR USR6A ON C6A.userId = USR6A.userId \n" +
                        "\t\t\tWHERE U6A.userId = C6A.connectionId AND C6A.status = 'Accept' AND (U6A.userId = '¬%s¬')\n" +
                        "\t\t)\n" +
                        "\t)\n" +
                        "\tUNION \n" +
                        "\tSELECT C4B.userId\n" +
                        "\tFROM USR U4B, CONNECTION_USR C4B\n" +
                        "\tINNER JOIN USR USR4B ON C4B.userId = USR4B.userId \n" +
                        "\tWHERE U4B.userId = C4B.connectionId AND C4B.status = 'Accept' AND (U4B.userId IN \n" +
                        "\t\t(\n" +
                        "\t\t\tSELECT C7A.connectionId\n" +
                        "\t\t\tFROM USR U7A, CONNECTION_USR C7A\n" +
                        "\t\t\tINNER JOIN USR USR7A ON C7A.connectionId = USR7A.userId \n" +
                        "\t\t\tWHERE U7A.userId = C7A.userId AND C7A.status = 'Accept' AND (U7A.userId = '¬%s¬')\n" +
                        "\t\t\tUNION \n" +
                        "\t\t\tSELECT C8A.userId\n" +
                        "\t\t\tFROM USR U8A, CONNECTION_USR C8A\n" +
                        "\t\t\tINNER JOIN USR USR8A ON C8A.userId = USR8A.userId \n" +
                        "\t\t\tWHERE U8A.userId = C8A.connectionId AND C8A.status = 'Accept' AND (U8A.userId = '¬%s¬')\n" +
                        "\t\t)\n" +
                        "\t)\n" +
                        "\t)\n" +
                        ");\n"
                , authorisedUser, authorisedUser, authorisedUser, authorisedUser, authorisedUser, authorisedUser,
                authorisedUser, authorisedUser, authorisedUser, authorisedUser, authorisedUser, authorisedUser,
                authorisedUser, authorisedUser).replace("¬", "");

        String alreadyFriendsQuery = String.format("SELECT C.connectionId\n" +
                "FROM USR U, CONNECTION_USR C\n" +
                "INNER JOIN USR USR1 ON C.connectionId = USR1.userId \n" +
                "WHERE U.userId = C.userId AND C.status = 'Accept' AND (U.userId = '¬%s¬')\n" +
                "UNION \n" +
                "SELECT C2.userId\n" +
                "FROM USR U2, CONNECTION_USR C2\n" +
                "INNER JOIN USR USR2 ON C2.userId = USR2.userId \n" +
                "WHERE U2.userId = C2.connectionId  AND C2.status = 'Accept' AND (U2.userId = '¬%s¬');", authorisedUser, authorisedUser).replace("¬", "");

        HashSet<String> alreadyFriendsSet = new HashSet<>();

        List<List<String>> alreadyFriendsResult = null;
        try {
            alreadyFriendsResult = esql.executeQueryAndReturnResult(alreadyFriendsQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (List<String> tuple : alreadyFriendsResult) {
            String userId = tuple.get(0).trim();
            alreadyFriendsSet.add(userId);
        }


        List<List<String>> usersResult = null;
        try {
            usersResult = esql.executeQueryAndReturnResult(possibleFriendIDSQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }


        HashSet<String> possibleUserNamesSet = new HashSet<>();

        for (List<String> tuple : usersResult) {
            String userId = tuple.get(0).trim();
            possibleUserNamesSet.add(userId);
        }

        if (!possibleUserNamesSet.contains(personId) && alreadyFriendsSet.size() > 5) {
            return false;
        }
        return true;
    }


}//end ProfNetwork
