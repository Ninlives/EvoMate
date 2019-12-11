package nl.mlatus.example;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        while (true){
            try {
                System.out.println("Enter the number of the user you want to query: ");
                Data.printUser(input.nextInt());
            } catch (InputMismatchException e){
                System.err.println("The input is not a valid number!");
                input.nextLine();
            }
        }
    }

}
