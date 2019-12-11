package nl.mlatus.example;

public class Data {
    static User[] users = {
            new User("John", "john19940206@gmail.com", 18),
            new User("Mary", "sns_mary@yahoo.com", 17),
            new User("Jane", "incrediblejane@gmail.com", 22),
            new User("Alice", "alice_in_wonderland@gmail.com", 19),
    };

    private static User getUser(int serialNumber){
        return users[serialNumber];
    }

    public static void printUser(int serialNumber){
        System.out.println(getUser(serialNumber));
    }
}
