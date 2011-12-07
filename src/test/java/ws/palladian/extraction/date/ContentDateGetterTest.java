package ws.palladian.extraction.date;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;

import ws.palladian.extraction.date.DateGetterHelper;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.technique.ContentDateGetter;
import ws.palladian.helper.StopWatch;
import ws.palladian.retrieval.DocumentRetriever;

public class ContentDateGetterTest {

	@Test
	public void testSplit(){
		String text = "a d";
		String[] parts = text.split("\\s");
		System.out.println(parts.length);
		for(int i=0; i< parts.length; i++){
			System.out.println("->" + parts[i] + "<-");
		}
	}
	
	@Test
	public void testGetDateTime(){
		ContentDateGetter dg = new ContentDateGetter();
		DocumentRetriever crawler = new DocumentRetriever();
		//String url = "data/evaluation/daterecognition/webpages/webpage_1292927290417.html";
		//String url = "data/evaluation/daterecognition/webpages/webpage_1294148721768.html";
		//String url = "data/evaluation/daterecognition/webpages/webpage_1294150309844.html"; //java out of memory: Java heap space
		//String url = "data/evaluation/daterecognition/webpages/webpage_1292928664726.html";
		String url = "http://chicknet.blogspot.com/2007/05/outerxml-for-java.html"; //Dates in Strucutre
		url = "http://grain.jouy.inra.fr/ggpages/awn/37/";
		url="http://www.archive.org/stream/liquidextraction030155mbp/liquidextraction030155mbp_djvu.txt";
		dg.setDocument(crawler.getWebDocument(url));
		

		StopWatch timer = new StopWatch();
		/*
		ArrayList<ContentDate> datesV1 = dg.getDates_old();
		
		System.out.println("V1 number of found Dates: " + datesV1.size());
		int cntKeywV1 = 0;
		for(ContentDate date : datesV1){
			String keyword = date.getKeyword();
			if(keyword != null && !keyword.equals("")){
				System.out.println(keyword + " dist: " + date.get(ContentDate.DISTANCE_DATE_KEYWORD) + " datePos: " + date.get(ContentDate.DATEPOS_IN_DOC) + " date: " + date.getDateString());
				cntKeywV1++;
			}
		}
		
		timer.stop();
		System.out.print("All over time V1: " );
		timer.getElapsedTimeString(true);
		System.out.print("keywords found V1: " + cntKeywV1);
		
		System.out.println();
		*/
		timer.start();
		
		ArrayList<ContentDate> datesV2 = dg.getDates();
		
		System.out.println("V2 number of found Dates: " + datesV2.size());
		int cntKeywV2 = 0;
		for(ContentDate date : datesV2){
			String keyword = date.getKeyword();
			if(keyword != null && !keyword.equals("")){
				System.out.println(" dist: " + date.get(ContentDate.DISTANCE_DATE_KEYWORD) + 
						" datePos: " + date.get(ContentDate.DATEPOS_IN_DOC) + 
						" date: " + date.getDateString() + 
						" keyword: " + date.getKeyword() + 
						" structDate: " + date.getStructureDateString());
				cntKeywV2++;
			}
		}
		
		timer.stop();
		System.out.println();
		System.out.print("All over time V2: " );
		timer.getElapsedTimeString(true);
		System.out.println("keywords found V2: " + cntKeywV2);

		
		
	}
	
	@Test
	public void testGetFindAllDatesTime(){
		String text = "";
		try{
			File file = new File("data/evaluation/daterecongnition/testFiles/text01.txt");
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
						
			String line;
			while((line = br.readLine())!= null){
				text += line;
			}
			br.close();
			fr.close();
		} catch(Exception e){
			
		}
		System.out.println(text.length());
		
		List<ContentDate> newDates = DateGetterHelper.findAllDates(text);
		HashMap<String, Integer> dateMap = new HashMap<String, Integer>(); 
		System.out.println(newDates.size());
		for(int i=0; i< newDates.size(); i++){
			if(dateMap.get(newDates.get(i).getDateString() + newDates.get(i).get(ContentDate.DATEPOS_IN_DOC)) != null){
				dateMap.put(newDates.get(i).getDateString() + newDates.get(i).get(ContentDate.DATEPOS_IN_DOC), 0);
			}else{
				dateMap.put(newDates.get(i).getDateString() + newDates.get(i).get(ContentDate.DATEPOS_IN_DOC), 1);
			}
		}
		int i=0;
		for(Entry<String, Integer> e: dateMap.entrySet()){
			if(e.getValue() == 1){
				System.out.println(e);
			}
			i++;
		}
		System.out.println(i);
		
	}

}