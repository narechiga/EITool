import java.io.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


class Parser {


public static String parseItem( String inputFile, String item ) {
	
	String text = "";

	// The patterns we will check for
	Pattern labelPattern = Pattern.compile( item );
	Pattern openBracePattern = Pattern.compile("{");
	Pattern textPattern = Pattern.compile("[^}]*");
	Pattern closeBracePattern = Pattern.compile("}");

	// States of the parser state machine
	int INIT	=	0;
	int LABELFOUND	=	1;
	int OPENBRACE	=	2;
	int TEXT	=	3;
	int CLOSEBRACE	=	4;
	int END		=	CLOSEBRACE;
	int ERROR	=	-1;
	
	int STATE = INIT;

	// assorted administrative overhead variables
	int labelEnd = 1000000;
	int openBraceEnd = 1000000;
	int textEnd = 1000000;
	int closeBraceEnd = 1000000;

	try {
		Scanner scan = new Scanner( new BufferedReader( new FileReader( inputFile ) ) );
	
		while ( (scan.hasNext()) && (STATE!= END) && (STATE != ERROR) ) {
			String tmpString = scan.next();

			// Run the matchers
			Matcher labelMatcher = labelPattern.matcher( tmpString );
			Matcher openBraceMatcher = openBracePattern.matcher( tmpString );
			Matcher textMatcher = textPattern.matcher( tmpString );
			Matcher closeBraceMatcher = closeBracePattern.matcher( tmpString );

			if ( STATE == INIT ) {
				if ( labelMatcher.find() ) {
					labelEnd = labelMatcher.end();
					STATE = LABELFOUND;
				}
			}

			if ( STATE == LABELFOUND ) {
				while ( openBraceMatcher.find() ) { //Cycle through open braces
					if ( openBraceMatcher.start() > labelEnd ) {
						openBraceEnd = openBraceMatcher.end();
						STATE = OPENBRACE;
					}
				}

			}

			if ( STATE == OPENBRACE ) {
				while ( textMatcher.find() ) {
					if ( textMatcher.start() > openBraceEnd ) {
						text = text + textMatcher.group();
						textEnd = textMatcher.end();
						STATE = TEXT;
					}
				}
			}

			if ( STATE == TEXT ) {
				while ( closeBraceMatcher.find() ) {
					if ( closeBraceMatcher.start() > textEnd ) {
						closeBraceEnd = closeBraceMatcher.end();
						STATE = CLOSEBRACE;
					}
				}

			}
			if ( STATE == CLOSEBRACE ) {
				//Do nothing, we are done here
			}
			if ( STATE == ERROR ) {
				System.out.println("Error!");
				return null;
			}

		}

	} catch ( Exception e ) {
		System.out.println( e );
	}

	return text;
}

public static String parseEnvelope( String inputFile ) {
	
	return parseItem( inputFile, "\\\\envelope" );
}

public static String parseInvariant( String inputFile ) {

	return parseItem( inputFile, "\\\\invariant" );

}

public static String parseRobustParameters( String inputFile ) {

	return parseItem( inputFile, "\\\\robustparameters" );

}

public static String parseControlLaw( String inputFile ) {

	return parseItem( inputFile, "\\\\robustparameters" );

}

}
