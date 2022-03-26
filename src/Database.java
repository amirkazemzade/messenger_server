import java.sql.*;

public class Database {

    /*** singleton ***/
    private static Database databaseInstance = null;

    private Database() {
        connectToDatabase();
    }

    public static Database getInstance() {
        if (databaseInstance == null) databaseInstance = new Database();
        return databaseInstance;
    }

    // the connection to the database
    private Connection connection;

    // connects to database and saves a connection variable in class
    private void connectToDatabase() {
        String url = "jdbc:sqlite:database.db";
        try {
            connection = DriverManager.getConnection(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // creates a new user in database
    public int insertUser(String username, String password) throws Exception {
        try {
            if (isUnique(username)) {
                Statement statement = connection.createStatement();
                statement.executeUpdate("INSERT INTO user (username, password) VALUES (\"" + username + "\", \"" + password + "\");");
                ResultSet resultSet = statement.executeQuery("SELECT id FROM user WHERE username=\"" + username + "\"");
                return resultSet.getInt("id");
            } else throw new Exception("username is not unique!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Something went wrong!");
        }
    }

    // checks if username is unique or not
    private boolean isUnique(String username) throws Exception {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM user WHERE username=\"" + username + "\";");
            return !resultSet.isBeforeFirst();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Something went wrong!");
        }
    }

    // checks if login information is valid or not
    public int login(String username, String password) throws Exception {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT id FROM user WHERE username=\"" + username + "\" and password=\"" + password + "\";");

            boolean valid = resultSet.isBeforeFirst();
            if (valid) {
                return resultSet.getInt("id");
            } else {
                throw new Exception("invalid username or password");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Something went wrong!");
        }
    }
}
