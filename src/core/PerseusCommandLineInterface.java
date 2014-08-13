
package perseus.core;

import java.io.*;
import java.util.*;
import perseus.abstractions.*;
import perseus.verification.*;

public class PerseusCommandLineInterface {

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static final String ANSI_BOLD = "\u001B[1m";
	
	PerseusInterfaceCore thisInterface;

	public PerseusCommandLineInterface( PerseusInterfaceCore thisInterface ) {
		this.thisInterface = thisInterface;
	}

	public void run() {
		VerificationProblem thisProblem = null;

		Scanner commandScanner = new Scanner( System.in );
		System.out.println("Type \"help\" for a list of available commands");
		while (true) {
			try {
				System.out.print( ANSI_BOLD +"perseus> " + ANSI_RESET);

				String input = commandScanner.nextLine();
				input = input.trim();
				Scanner in = new Scanner( input );

				if ( in.hasNext("find-instance") ){
					in.skip("find-instance");
					thisInterface.findInstance( in.nextLine() + "\n" );

				} else if ( in.hasNext("load") ) { 
					in.skip("load");
					thisProblem = thisInterface.loadFromFile( in.nextLine().trim() );

				} else if ( in.hasNext("print") ) { 
					System.out.println( thisProblem.toString() );
					in.nextLine();
					
				} else if ( in.hasNext("auto-refine") ) { 
					thisInterface.autoRefine( thisProblem );
					in.nextLine();

				} else if ( in.hasNext("auto-parts") ) { 
					thisInterface.autoParts( thisProblem );

				} else if ( in.hasNext("propose-refine") ) { 
					if ( thisInterface.proposeRefine( thisProblem ) ) {
						System.out.println(ANSI_BOLD + ANSI_GREEN + "Success!" + ANSI_RESET);
					} else {
						System.out.println(ANSI_BOLD + ANSI_YELLOW + "Failure." + ANSI_RESET);
					}

					in.nextLine();

				} else if ( in.hasNext("propose-parts") ) { 
					if ( thisInterface.proposeParts( thisProblem ) ) {
						System.out.println(ANSI_BOLD + ANSI_GREEN + "Success!" + ANSI_RESET);
					} else {
						System.out.println(ANSI_BOLD + ANSI_YELLOW + "Failure." + ANSI_RESET);
					}
					in.nextLine();

				} else if ( in.hasNext("clear") ) { 
					thisProblem = null;
					in.nextLine();

				} else if ( in.hasNext("version") ) { 
					thisInterface.printVersion();
					in.nextLine();

				} else if ( in.hasNext("help") ) {
					thisInterface.giveHelp();
					in.nextLine();

				} else if ( in.hasNext("parse") ) {
					in.skip("parse");
					thisInterface.runParser( in.nextLine() + "\n" );
				} else if ( in.hasNext("exit") ) {
					System.exit(0);
				} else if ( in.hasNext("quit") ) {
					System.exit(0);
				} else if ( in.hasNext() ) {
					System.out.println("I don't know how to do that.");
					in.nextLine();
				} else {
				}

		    	} catch ( Exception e ) { 
				System.out.println("Error running parser test loop.");
				System.err.println( e );
				e.printStackTrace();
			}
		}

	}
}
