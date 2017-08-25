package de.bitsharesmunich.models;

import junit.framework.Assert;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by nelson on 4/4/17.
 */
public class BlocktradesSessionResponseTest {

    @Test
    public void testDateParse(){
        String sampleDateString = "2017-04-05T02:10:24.608641+00:00";
        SimpleDateFormat dateFormat = new SimpleDateFormat(BlocktradesSessionResponse.EXPIRATION_TIME_FORMAT);
        try {
            Date date = dateFormat.parse(sampleDateString);
            System.out.println("date: "+date.toString());
            Assert.assertNotNull(date);
        } catch (ParseException e) {
            System.out.println("ParseException. Msg: "+e.getMessage());
        }
    }
}