import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Generates a graph of the Covid-19 pandemic outbreak data of Total Cases or New Cases. Will then create a projection
 * of a given number of days after the current date.
 *
 * @author Winston Riggs
 * @version 1.2
 */

public class Covid {
    public static void main(String[] args) {
        try {
            //Creates an ArrayList to store covid-19 data from api
            ArrayList<Integer> dataVal = new ArrayList<>();
            int dataType;
            String dataTypeInput;
            int scale;
            int lengthOfProjection;

            //Gets the date of the previous day as the api is one day behind
            LocalDateTime now = LocalDateTime.of(2020, 8, 18, 11, 57);
            Scanner keyboard = new Scanner(System.in);
            String state;

            //Asks user about location desired
            System.out.println("Would you like to view data for a state or the country? \n(For country type \"US\" and the two digit symbol for a state ex. \"NY\")");
            state = keyboard.nextLine();

            while(!isStateValid(state)) {
                System.out.println("Sorry. That is not a valid input. Please try again.");
                state = keyboard.nextLine();
            }

            //Asks user about data type desired
            System.out.println("Would you like to view new cases per day or total cases? \n0 - New Cases \n1 - Active Cases");
            dataTypeInput = keyboard.nextLine();

            while (!dataTypeInput.equals("0") && !dataTypeInput.equals("1")) {
                System.out.println("Sorry. That is not a valid input. Please try again.");
                dataTypeInput = keyboard.nextLine();
            }

            dataType = Integer.parseInt(dataTypeInput);

            //Asks user about the projection feature
            System.out.println("How many days of projections would you like?");
            lengthOfProjection = keyboard.nextInt();

            keyboard.close();

            System.out.println("Please Wait. Fetching Data . . .");

            //Sorts data from the api into an ArrayList
            for (int i = 0; i < 150; i++) {
                dataVal.add(0, getData(formatDate(now), dataType, state.toUpperCase()));
                now = now.minusDays(1);
            }

            //Smooths out the data with a moving average for a cleaner projection
            ArrayList<Double> movingAverage = calcMovingAverage(15, dataVal);

            //Calculates a projection
            double curChange = getChange(movingAverage);
            double constantChange = getConstantChange(movingAverage);
            double startVal = dataVal.get(dataVal.size() - 1);

            //Finds a comfortable scale for the graph
            scale = (int) (findMax(dataVal) * 0.02);
            if (scale == 0)
                scale = 1;

            //Sorts the projection into an ArrayList
            ArrayList<Integer> projection = getProjection(startVal, lengthOfProjection, curChange, constantChange);

            LocalDateTime time = LocalDateTime.now().minusDays(61);

            //Prints out the real covid-19 data
            for (int i : dataVal) {
                System.out.println(formatDateForUser(time) + ": " + generateGraph(i, scale));
                time = time.plusDays(1);
            }

            System.out.println("----------------------------------------");

            //Prints out the projection data
            for (int i : projection) {
                System.out.println(formatDateForUser(time) + ": " + generateGraph(i, scale));
                time = time.plusDays(1);
            }

            String typeOfData;

            //Prints out further details about data
            if (dataType == 0)
                typeOfData = "New Cases";
            else
                typeOfData = "Total Cases";

            System.out.println("\n| = " + scale + " " + typeOfData);

        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    /**
     * Will check if the given String is a state abbreviation or United States abbreviation. Uses linear search.
     * @param input A String to be tested with
     * @return boolean value of true if the given input is a valid state and false otherwise
     */
    public static boolean isStateValid(String input) {
        //Array of all states and US
        String[] states = {"US", "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"};

        //Checks if the input is a valid state or US
        for (String state: states)
            if (input.toUpperCase().equals(state))
                return true;
        return false;
    }

    /**
     * Will find the maximum of an unsorted ArrayList using linear search.
     * @param vals An ArrayList of Integers that represents the data
     * @return The index of the maximum value
     */
    public static int findMax (ArrayList<Integer> vals) {
        int max = Integer.MIN_VALUE;

        //Parses through array to find the maximum value
        for (int val : vals)
            if (val > max)
                max = val;
        return max;
    }

    /**
     * Will generate a projection of a graph. 
     * @param startingVal An int of the starting value
     * @param size An int of the length of the projection
     * @param curChange A double representing the first rate of change for the function
     * @param secChange A double representing the second rate of change for the function
     * @return An ArrayList of Integers representing the projection of the function
     */
    public static ArrayList<Integer> getProjection(double startingVal, int size, double curChange, double secChange) {
        ArrayList<Integer> projection = new ArrayList<>();

        //Iterates the length of the desired projection and updates after 3 values
        for (int i = 0; i < size; i = i + 3) {

            //Temporary array to store the 3 projected values
            int[] temp = new int[3];

            //Calculates the next three projected values
            for (int j = 0; j < 3; j++) {
                temp[j] = (int)(startingVal + curChange);
                projection.add(temp[j]);
                startingVal = temp[j];
            }

            curChange += secChange;
        }

        return projection;
    }

    /**
     * Will get the average of the first rate of change for the last 9 data points of a given data set.
     * @param dataPoints An ArrayList of Integers representing the data points of a function
     * @return A double representing the average of the first rate of change for the last 9 data values
     */
    public static double getChange(ArrayList<Double> dataPoints) {
        double change = 0;

        //Finds the net changes for the previous 8 values in the data set
        for (int i = dataPoints.size() - 8; i < dataPoints.size() - 1; i++)
            change += dataPoints.get(i + 1) - dataPoints.get(i);

        //returns the average change
        return change / (dataPoints.size() - 1);
    }

    /**
     * Will get the average of the second rate of change for the last 6 data points of a given data set.
     * @param dataPoints An ArrayList of Integers representing the data points of a function
     * @return A double representing the average of the second rate of change for the last 6 data values
     */
    public static double getConstantChange(ArrayList<Double> dataPoints) {
        ArrayList<Double> changes = new ArrayList<>();
        ArrayList<Double> secChange = new ArrayList<>();

        for (int i = dataPoints.size() - 7; i < dataPoints.size() - 1; i++)
            changes.add(dataPoints.get(i + 1) - dataPoints.get(i));

        for (int i = 0; i < changes.size() - 1; i++)
            secChange.add(changes.get(i + 1) - changes.get(i));

        double avg = 0.0;

        for (double i : secChange)
            avg += i;

        avg /= secChange.size();

        return avg;
    }

    /**
     * Will format the date in a yyyyMMdd format.
     * @param now A LocalDateTime variable representing the date to be formatted
     * @return A String of the formatted date
     */
    public static String formatDate(LocalDateTime now) {
        DateTimeFormatter format1 = DateTimeFormatter.ofPattern("yyyyMMdd");
        return now.format(format1);
    }

    /**
     * Will format the date in a MM/dd/yyyy format.
     * @param now A LocalDateTime variable representing the date to be formatted
     * @return A String of the formatted date
     */
    public static String formatDateForUser (LocalDateTime now) {
        DateTimeFormatter format1 = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return now.format(format1);
    }

    /**
     * Will GET data from the covidtracking api.
     * @param date A String of the date to get the data from
     * @param dataType The type of data requested (0 - New Cases and 1 - Active Cases)
     * @param state The two letter abbreviation of the US state requested or US for the country
     * @return An int representing the requested data from the inputs
     * @throws IOException e
     */
    public static int getData(String date, int dataType, String state) throws IOException {
        String url;
        if (state.equals("US"))
            url = "https://covidtracking.com/api/v1/us/" + date + ".json";
        else
            url = "https://covidtracking.com/api/v1/states/" + state.toLowerCase() + "/" + date + ".json";
        URL link = new URL(url);
        HttpURLConnection con = (HttpURLConnection) link.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();
        String data = response + "";

        if (dataType == 0)
            return getCases(data);
        return getTotal(data);
    }

    /**
     * Will parse through a string find the number of new cases.
     * @param data The String representation of a Json file from the covidtracking api
     * @return An int of the number of new cases in the given data
     */
    public static int getCases(String data) {
        String tempHolder = data.substring(data.indexOf("positiveIncrease"));
        String numNewCases = tempHolder.substring(tempHolder.indexOf(":") + 1, tempHolder.indexOf(","));
        return Integer.parseInt(numNewCases);
    }

    /**
     * Will parse through a string find the number of active cases.
     * @param data The String representation of a Json file from the covidtracking api
     * @return An int of the number of active cases in the given data
     */
    public static int getTotal(String data) {
        String tempHolder = data.substring(data.indexOf("positive"));
        String numTotalCases = tempHolder.substring(tempHolder.indexOf(":") + 1, tempHolder.indexOf(","));
        return Integer.parseInt(numTotalCases);
    }

    /**
     * Will generate a given number of "|" to represent a graph.
     * @param cases The Y-value to generate the graph
     * @param scale The scale of how much a "|" will represent
     * @return A String of the generated graph
     */
    public static String generateGraph(int cases, int scale) {
        StringBuilder returnVal = new StringBuilder();
        if (cases >= scale) {
            cases = cases / scale;
            returnVal.append("|".repeat(Math.max(0, cases)));
        }
        return returnVal.toString();
    }

    public static ArrayList<Double> calcMovingAverage(int length, ArrayList<Integer> dataSet) {
        ArrayList<Double> movingAverage = new ArrayList<>();

        if (length > dataSet.size())
            return movingAverage;

        for (int i = 0; i < dataSet.size() - (length - 1); i++) {
            double ttl = 0;
            for (int j = i; j < i + length; j++) {
                ttl = ttl + dataSet.get(j);
            }

            movingAverage.add(ttl/length);
        }

        return movingAverage;
    }
}