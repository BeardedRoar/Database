package edu.chl.lab5;

/* This is the driving engine of the program. It parses the command-line
 * arguments and calls the appropriate methods in the other classes.
 *
 * You should edit this file in two ways:
 * 1) Insert your database username and password in the proper places.
 * 2) Implement the three functions getInformation, registerStudent
 *    and unregisterStudent.
 */
import java.sql.*; // JDBC stuff.
import java.util.Properties;
import java.io.*;  // Reading user input.

public class StudentPortal
{
    /* TODO Here you should put your database name, username and password */
    static final String USERNAME = UserInfo.USERNAME;
    static final String PASSWORD = UserInfo.PASSWORD;

    /* Print command usage.
     * /!\ you don't need to change this function! */
    public static void usage () {
        System.out.println("Usage:");
        System.out.println("    i[nformation]");
        System.out.println("    r[egister] <course>");
        System.out.println("    u[nregister] <course>");
        System.out.println("    q[uit]");
    }

    /* main: parses the input commands.
     * /!\ You don't need to change this function! */
    public static void main(String[] args) throws Exception
    {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://ate.ita.chalmers.se/";
            Properties props = new Properties();
            props.setProperty("user",USERNAME);
            props.setProperty("password",PASSWORD);
            Connection conn = DriverManager.getConnection(url, props);

            String student = args[0]; // This is the identifier for the student.

            Console console = System.console();
            usage();
            System.out.println("Welcome!");
            while(true) {
                String mode = console.readLine("? > ");
                String[] cmd = mode.split(" +");
                cmd[0] = cmd[0].toLowerCase();
                if ("information".startsWith(cmd[0]) && cmd.length == 1) {
                    /* Information mode */
                    getInformation(conn, student);
                } else if ("register".startsWith(cmd[0]) && cmd.length == 2) {
                    /* Register student mode */
                    registerStudent(conn, student, cmd[1]);
                } else if ("unregister".startsWith(cmd[0]) && cmd.length == 2) {
                    /* Unregister student mode */
                    unregisterStudent(conn, student, cmd[1]);
                } else if ("quit".startsWith(cmd[0])) {
                    break;
                } else usage();
            }
            System.out.println("Goodbye!");
            conn.close();
        } catch (SQLException e) {
            System.err.println(e);
            System.exit(2);
        }
    }

    /* Given a student identification number, this function should print
     * - the name of the student, the students national identification number
     *   and their issued login name (something similar to a CID)
     * - the programme and branch (if any) that the student is following.
     * - the courses that the student has read, along with the grade.
     * - the courses that the student is registered to. (queue position if the student is waiting for the course)
     * - the number of mandatory courses that the student has yet to read.
     * - whether or not the student fulfills the requirements for graduation
     */
    static void getInformation(Connection conn, String student) throws SQLException
    {
        Statement st = conn.createStatement();
        
        
        // Start by printing the students personal info
        System.out.println("Information for student " + student + "\n-------------------------------------");
        
        ResultSet personalInfo = st.executeQuery("SELECT * FROM StudentsFollowing WHERE NationalIDNbr='" + student + "'");
        if(personalInfo.next()){
        	System.out.println("Name: " + personalInfo.getString(4));
        	System.out.println("Student ID: " + personalInfo.getString(3));
        	System.out.println("Line: " + personalInfo.getString(5));
        	String branch = personalInfo.getString(6);
        	if (branch != null) {
        		System.out.println("Branch: " + branch);
        	} else {
        		System.out.println("Branch: None");
        	}
        }
        personalInfo.close();
        
        
        // Then print the read course(s)
        System.out.println("\nRead courses (name (code), credits: grade):");
        
        ResultSet readCourses = st.executeQuery("SELECT * FROM FinishedCourses WHERE NationalIDNbr='" + student + "'");
        while(readCourses.next()){
        	System.out.println(" " + readCourses.getString(4) + " (" + readCourses.getString(3)+ "), "
        			+ readCourses.getString(6) + "p: " + readCourses.getString(5));
        }
        readCourses.close();
        
        
        // The print the course(s) the student is registered to
        System.out.println("\nRegistered courses (name (code): status):");
        
        ResultSet registredCourses = st.executeQuery("SELECT * FROM Registrations WHERE NationalIDNbr='" + student + "'");
        Statement st2 = conn.createStatement();//We need a separate statement to have 2 different ResultSet open
        while(registredCourses.next()){
        	String queuStatus = registredCourses.getString(4);
        	String courseID = registredCourses.getString(3);
        	String courseName = "";
        	
        	ResultSet course = st2.executeQuery("SELECT * FROM Course WHERE ID='" + courseID + "'");
        	if (course.next()) {
        		courseName=course.getString(2);
        	}
        	course.close();
        	
        	System.out.println(" " + courseName + " (" + courseID + "): " + queuStatus);
        }
        st2.close();
        registredCourses.close();
        
        
        // Finally print how close the student is to graduation
        ResultSet pathToGraduation = st.executeQuery("SELECT * FROM PathToGraduation WHERE NationalIDNbr='" + student + "'");
        if (pathToGraduation.next()) {
        	System.out.println("\nSeminar courses taken: " + pathToGraduation.getString(8));
        	System.out.println("Math credits taken: " + pathToGraduation.getString(6));
        	System.out.println("Research credits taken: " + pathToGraduation.getString(7));
        	System.out.println("Recomended credits taken: " + pathToGraduation.getString(4));
        	System.out.println("Total credits taken: " + pathToGraduation.getString(3));
        	System.out.println("Mandatory credits needed for graduation: " + pathToGraduation.getString(5));
        	System.out.println("Fulfills the requirements for graduation: " + pathToGraduation.getString(9));
        }
        pathToGraduation.close();
        
        System.out.println("-------------------------------------");
        st.close();
    }

    /* Register: Given a student id number and a course code, this function
     * should try to register the student for that course.
     */
    static void registerStudent(Connection conn, String student, String course)
            throws SQLException
    {
    	PreparedStatement st = conn.prepareStatement("INSERT INTO Registrations VALUES(? , 'NULL', ?)");
    	st.setString(1, student);
    	st.setString(2, course);
    	
    	try {
    		st.executeUpdate();
        	System.out.println("Registration completed");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
    	
    	st.close();
    }

    /* Unregister: Given a student id number and a course code, this function
     * should unregister the student from that course.
     */
    static void unregisterStudent(Connection conn, String student, String course)
            throws SQLException
    {
    	PreparedStatement st = conn.prepareStatement("DELETE FROM Registrations WHERE nationalIDNbr=? AND courseID=?");
    	st.setString(1, student);
    	st.setString(2, course);
    	
    	try {
    		st.executeUpdate();
        	System.out.println("Unegistration completed");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
    	
    	st.close();
    }
}
