package database;

import Exceptions.MyServerException;
import models.GroupMessage;
import models.PrivateMessage;
import models.Session;

import java.security.SecureRandom;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

public class Database {

    /*** singleton ***/
    private static Database databaseInstance = null;
    // the connection to the database
    private Connection connection;

    private Database() {
        connectToDatabase();
    }

    public static Database getInstance() {
        if (databaseInstance == null) databaseInstance = new Database();
        return databaseInstance;
    }

    // connects to database and saves a connection variable in class
    private void connectToDatabase() {
        String url = "jdbc:sqlite:database.db";
        try {
            connection = DriverManager.getConnection(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*** User ***/

    // creates a new user in database
    public String insertUser(String username, String password) throws MyServerException {
        try {
            if (!isUserExist(username)) {
                Statement statement = connection.createStatement();
                statement.executeUpdate(
                        "INSERT INTO user (username, password) VALUES (\"" + username + "\", \"" + password + "\");"
                );
                return username;
            } else throw new MyServerException("username is not unique!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong!");
        }
    }

    // checks if a user with this username does exist
    private boolean isUserExist(String username) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    "SELECT * FROM user WHERE username=\"" + username + "\";"
            );
            return resultSet.isBeforeFirst();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong!");
        }
    }

    // searches for a user with username and returns its id,
    // if user not founded it will return -1
    private int findUser(String username) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    "SELECT id FROM user WHERE username=\"" + username + "\";"
            );
            if (resultSet.isBeforeFirst()) return resultSet.getInt("id");
            else return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong!");
        }
    }

    // finds username of user with user id,
    // returns null if no user were found
    private String getUsername(int userId) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    String.format(
                            "select username from user where id = %d",
                            userId
                    )
            );
            if (resultSet.isBeforeFirst()) return resultSet.getString("username");
            else return null;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong!");
        }
    }

    // checks if sid is valid,
    // returns username if it found user and returns null if it does not
    public String validate(String sid) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    String.format(
                            "SELECT * FROM user_session, user " +
                                    "WHERE sid=\"%s\" " +
                                    "AND user.id = user_session.user_id " +
                                    "ORDER BY user_session.created_at DESC ",
                            sid
                    )
            );
            if (resultSet.isBeforeFirst()) {
                return resultSet.getString("username");
            } else return null;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    // checks if a user with this username exist or not,
    // returns true if it does exist and false if it doesn't
    public boolean doesUserExist(String username) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    String.format(
                            "SELECT * FROM user " +
                                    "WHERE username=\"%s\"",
                            username
                    )
            );
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    // checks if login information is valid or not
    public Session login(String username, String password) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    "SELECT id FROM user WHERE username=\"" + username + "\" and password=\"" + password + "\";"
            );

            boolean valid = resultSet.isBeforeFirst();
            if (valid) {
                int id = resultSet.getInt("id");
                Statement newSessionStatement = connection.createStatement();
                boolean isSIDUnique = false;
                String sid = "";
                while (!isSIDUnique) {
                    sid = generateSID();
                    ResultSet sidResult = newSessionStatement.executeQuery(
                            String.format("SELECT sid from user_session WHERE sid = \"%s\"", sid)
                    );
                    if (!sidResult.isBeforeFirst()) isSIDUnique = true;
                }
                newSessionStatement.executeUpdate(
                        String.format(
                                "INSERT INTO user_session (user_id, sid) " +
                                        "Values (%d, \"%s\");",
                                id,
                                sid
                        )
                );
                return new Session(username, sid);
            } else {
                throw new MyServerException("invalid username or password");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong!");
        }
    }

    // generates a random sid for user authentication
    private String generateSID() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[20];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().encodeToString(randomBytes);
    }

    /*** Private Message ***/

    // saves a private message to database
    public void savePM(String senderUsername, String receiverUsername, String message) throws MyServerException {
        try {
            int receiverId = findUser(receiverUsername);
            if (receiverId != -1) {
                int senderId = findUser(senderUsername);
                if (senderId != -1) {
                    Statement statement = connection.createStatement();
                    statement.executeUpdate(
                            "INSERT INTO private_message (sender_id, receiver_id, message) " +
                                    "VALUES (\"" + senderId + "\", \"" + receiverId + "\", \"" + message + "\")"
                    );

                } else throw new MyServerException("sender not found");
            } else throw new MyServerException("receiver user not found");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // returns all messages of given user
    public ArrayList<PrivateMessage> getAllPrivateMessages(String username) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    String.format(
                            "SELECT * FROM private_message, user " +
                                    "WHERE (user.username = \"%s\") " +
                                    "AND (user.id = private_message.receiver_id OR user.id = private_message.sender_id)",
                            username
                    )
            );

            return getPrivateMessages(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    // returns all messages of given user with wanted user
    public ArrayList<PrivateMessage> getAllPrivateMessages(String requestedUser, String wantedUser) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet requestedUserResult = statement.executeQuery(String.format(
                    "SELECT id FROM user WHERE username=\"%s\"",
                    requestedUser
            ));
            int requestedUserId = requestedUserResult.getInt("id");

            ResultSet wantedUserResult = statement.executeQuery(String.format(
                    "SELECT id FROM user WHERE username=\"%s\"",
                    wantedUser
            ));
            int wantedUserId = wantedUserResult.getInt("id");

            ResultSet resultSet = statement.executeQuery(
                    String.format(
                            "SELECT * FROM private_message " +
                                    "WHERE (sender_id = %d AND receiver_id = %d) " +
                                    "OR (sender_id = %d AND receiver_id = %d)",
                            requestedUserId, wantedUserId,
                            wantedUserId, requestedUserId
                    )
            );

            return getPrivateMessages(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    // extracts private messages from given database query result set,
    // returns a list of messages as PrivateMessage model
    private ArrayList<PrivateMessage> getPrivateMessages(ResultSet resultSet) throws SQLException, MyServerException {
        ArrayList<PrivateMessage> messages = new ArrayList<>();
        while (resultSet.next()) {
            int senderId = resultSet.getInt("sender_id");
            int receiverId = resultSet.getInt("receiver_id");
            String sendTimeString = resultSet.getString("created_at");
            String message = resultSet.getString("message");

            String senderUsername = getUsername(senderId);
            String receiverUsername = getUsername(receiverId);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date sendTime;
            try {
                sendTime = formatter.parse(sendTimeString);
            } catch (ParseException e) {
                e.printStackTrace();
                throw new MyServerException("Something went wrong");
            }
            messages.add(
                    new PrivateMessage(
                            senderUsername, receiverUsername, message.getBytes().length, message, sendTime.getTime()
                    )
            );
        }
        return messages;
    }

    private boolean doesGroupExist(String groupId) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(String.format("SELECT group_id from \"group\" where group_id = \"%s\"", groupId));
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    private int getGroupTableId(String groupId) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(String.format("SELECT id from \"group\" where group_id = \"%s\"", groupId));
            if (resultSet.isBeforeFirst()) return resultSet.getInt("id");
            else return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    public void insertNewGroup(String groupId, String ownerUsername) throws MyServerException {
        int ownerId = findUser(ownerUsername);
        if (ownerId == -1) throw new MyServerException("Owner user does not exist");
        try {
            if (doesGroupExist(groupId)) throw new MyServerException("This group id is already taken");
            Statement statement = connection.createStatement();
            statement.executeUpdate(String.format("INSERT INTO \"group\" (group_id, owner_id) VALUES (\"%s\", %d)", groupId, ownerId));
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    public void addUserToGroup(String groupId, String username) throws MyServerException {
        int groupTableId = getGroupTableId(groupId);
        if (groupTableId == -1) throw new MyServerException("No group with this id was found");
        int userTableId = findUser(username);
        if (userTableId == -1) throw new MyServerException("No user with this username exists");
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    String.format(
                            "SELECT * FROM group_users where group_id = %d and user_id = %d",
                            groupTableId, userTableId
                    )
            );
            if (resultSet.isBeforeFirst()) throw new MyServerException("User has already joined the group");
            statement.executeUpdate(String.format("INSERT INTO group_users (group_id, user_id) VALUES (%d, %d)", groupTableId, userTableId));
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    public void saveGM(String groupId, String senderUsername, String receiverUsername, String message) throws MyServerException {
        int groupTableId = getGroupTableId(groupId);
        if (groupTableId == -1) throw new MyServerException("No group with this id was found");
        String senderIdString = null;
        String receiverIdString = null;
        int senderId;
        int receiverId;
        if (senderUsername != null) {
            senderId = findUser(senderUsername);
            if (senderId == -1) throw new MyServerException("No user with this username exists");
            senderIdString = String.valueOf(senderId);
        }
        if (receiverUsername != null) {
            receiverId = findUser(receiverUsername);
            if (receiverId == -1) throw new MyServerException("No user with this username exists");
            receiverIdString = String.valueOf(receiverId);
        }
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(
                    String.format(
                            "INSERT INTO group_message (group_id, receiver_id, message, sender_id) " +
                                    "VALUES (%d, \"%s\", \"%s\", \"%s\")",
                            groupTableId, receiverIdString, message, senderIdString
                    )
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    public ArrayList<String> getGroupUsers(String groupId) throws MyServerException {
        int groupTableId = getGroupTableId(groupId);
        if (groupTableId == -1) throw new MyServerException("No group with this id was found");
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    String.format(
                            "SELECT * FROM user, group_users WHERE user.id = group_users.user_id AND group_id = %d",
                            groupTableId
                    )
            );
            ArrayList<String> users = new ArrayList<>();
            while (resultSet.next()){
                users.add(resultSet.getString("username"));
            }
            return users;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    public ArrayList<GroupMessage> getAllGroupMessages(String username) throws MyServerException {
        int userId = findUser(username);
        if (userId == -1) throw new MyServerException("No user with this username exists");
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    String.format(
                            "select * from group_message " +
                                    "where group_id in (SELECT group_id from group_users where user_id = %d) " +
                                    "and (receiver_id is null or receiver_id = \"null\" or receiver_id = %d)",
                            userId, userId
                    )
            );
            return getGroupMessages(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }
    public ArrayList<GroupMessage> getAllGroupMessagesFrom(String username, String groupId) throws MyServerException {
        int groupTableId = getGroupTableId(groupId);
        if (groupTableId == -1) throw new MyServerException("No group with this id was found");
        int userId = findUser(username);
        if (userId == -1) throw new MyServerException("No user with this username exists");
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    String.format(
                            "select * from group_message " +
                                    "where group_id in " +
                                    "(SELECT group_id from group_users " +
                                    "where user_id = %d and group_users.group_id = %d) " +
                                    "and (receiver_id is null or receiver_id = \"null\" or receiver_id = %d)",
                            userId, groupTableId, userId
                    )
            );
            return getGroupMessages(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong");
        }
    }

    private ArrayList<GroupMessage> getGroupMessages(ResultSet resultSet) throws SQLException, MyServerException {
        ArrayList<GroupMessage> groupMessages = new ArrayList<>();
        while (resultSet.next()){
            int messageGroupId = resultSet.getInt("group_id");
            String receiverId = resultSet.getString("receiver_id");
            String message = resultSet.getString("message");
            String senderId = resultSet.getString("sender_id");
            String sendTimeString = resultSet.getString("created_at");

            String senderUsername = null;
            String receiverUsername = null;
            if (senderId != null){
                if (!senderId.equals("null"))
                    senderUsername = getUsername(Integer.parseInt(senderId));
            }
            if (receiverId != null){
                if (!receiverId.equals("null"))
                    receiverUsername = getUsername(Integer.parseInt(receiverId));
            }
            String groupId = getGroupId(messageGroupId);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date sendTime;
            try {
                sendTime = formatter.parse(sendTimeString);
            } catch (ParseException e) {
                e.printStackTrace();
                throw new MyServerException("Something went wrong");
            }

            groupMessages.add(
                    new GroupMessage(groupId, senderUsername, sendTime.getTime(), receiverUsername, message)
            );
        }
        return groupMessages;
    }

    private String getGroupId(int groupIndex) throws MyServerException {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    String.format(
                            "SELECT group_id FROM \"group\" WHERE id = %d",
                            groupIndex
                    )
            );
            if (resultSet.isBeforeFirst()) return resultSet.getString("group_id");
            else return null;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MyServerException("Something went wrong!");
        }
    }
}
