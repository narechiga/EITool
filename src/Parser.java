import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


class Parser {


public static String parseItem( String inputFile, String item ) {
	
	String text = "";

	// The patterns we will check for
	Pattern labelPattern = Pattern.compile( item );
	Pattern textPattern = Pattern.compile("[^\\}\\s]*");
	Pattern endPattern = Pattern.compile("end");

	// States of the parser state machine
	int INIT		=	1;
	int LABELFOUND		=	2;
	int TEXTFOUND		=	3;
	int END			=	4;
	
	int STATE = INIT;

	try {
		//System.out.println("Opening file scanner...");
		Scanner scan = new Scanner( new BufferedReader( new FileReader( inputFile ) ) );
	
		while ( (scan.hasNext()) && (STATE!= END) ) {
			String tmpString = scan.next();
			tmpString = tmpString.replace("\\n", ""); // Kill the evil newlines

			//System.out.println("Input line is: " + tmpString + "; Current state is: " + STATE);

			// Run the matchers
			Matcher labelMatcher = labelPattern.matcher( tmpString );
			Matcher textMatcher = textPattern.matcher( tmpString );
			Matcher endMatcher = endPattern.matcher( tmpString );
			
			switch( STATE ) {

				case 1: // INIT
					//System.out.println("Parser automaton initialized");
					if ( labelMatcher.find() ) {
						STATE = LABELFOUND;

					//	System.out.println("Label found, ending at position " + labelMatcher.end() + ": " + labelMatcher.group());
					}
					break;

				case 2: // LABELFOUND
					if ( textMatcher.find() ) {
						STATE = TEXTFOUND;

						text = textMatcher.group();
					//	System.out.println("Text found, ending at position " + textMatcher.end() + ": " + text );
					}
					break;
					

				case 3: // TEXTFOUND

					if ( endMatcher.find() ) {
						STATE = END;

					//	System.out.println("End found, ending at position " + endMatcher.end() + ": " + endMatcher.group() );
					} else if ( textMatcher.find() ) {
						text = text + " " + textMatcher.group();
						STATE = TEXTFOUND;

					//	System.out.println("Text found, ending at position " + textMatcher.end());
					//	System.out.println("Text is: " + text );
					} 

					break;

				case 4: // END
					break;

				}
		}
	} catch ( Exception e ) {
		System.out.println( e );
	}

	return text;
}

public static String parseEnvelope( String inputFile ) {
	
	//System.out.println("Starting parse for envelope...");
	return parseItem( inputFile, "envelope:" );
}

public static String parseInvariant( String inputFile ) {

	//System.out.println("Starting parse for invariant...");
	return parseItem( inputFile, "invariant:" );

}

public static String parseRobustParameters( String inputFile ) {

	//System.out.println("Starting parse for robust parameters...");
	return parseItem( inputFile, "robustparameters:" );

}

public static String parseControlLaw( String inputFile ) {

	//System.out.println("Starting parse for control law...");
	return parseItem( inputFile, "controllaw:" );

}

}
