package org.atlasapi.remotesite.pa;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

public class PaDeltaFileHelperTest {

    private PaDeltaFileHelper deltaFileHelper;
    
    @Before
    public void setUp() {
        deltaFileHelper = new PaDeltaFileHelper();
    }
    
    @Test
    public void testVersionNumberSnapshotFile() {
        File file = new File("/data/pa/20111227_tvdata.xml");
        assertThat(deltaFileHelper.versionNumber(file), is(1L));
    }
    
    @Test
    public void testVersionNumberDeltaFile() {
        File file = new File("201111101545_20111110_tvdata.xml");
        assertThat(deltaFileHelper.versionNumber(file), is(201111101545L));
    }
    
    @Test
    public void testGroupAndOrderFilesByDay() throws Exception {
		File file1 = new File(new URI("file:/data/pa/TV/201101010145_20110102_tvdata.xml"));
		File file2 = new File(new URI("file:/data/pa/TV/20110101_tvdata.xml"));
		File file3 = new File(new URI("file:/data/pa/TV/201101040130_20110101_tvdata.xml"));
		File file4 = new File(new URI("file:/data/pa/TV/201101050145_20110101_tvdata.xml"));
		File file5 = new File(new URI("file:/data/pa/TV/20110102_tvdata.xml"));
		File file6 = new File(new URI("file:/data/pa/TV/201101010200_20110102_tvdata.xml"));
		File file7 = new File(new URI("file:/data/pa/TV/20110103_tvdata.xml"));
		File file8 = new File(new URI("file:/data/pa/TV/201101011930_20110102_tvdata.xml"));
        
        List<File> files = new ArrayList<File>();
        files.add(file1);
        files.add(file2);
        files.add(file3);
        files.add(file4);
        files.add(file5);
        files.add(file6);
        files.add(file7);
        files.add(file8);
        
        Set<Queue<File>> groupedFiles = deltaFileHelper.groupAndOrderFilesByDay(files);
        assertEquals(3, groupedFiles.size());
        
        for(Queue<File> filesForDay : groupedFiles) {
            if(filesForDay.size() == 3) {
                assertEquals(file2.toURI().toString(), filesForDay.remove().toURI().toString());
                assertEquals(file3.toURI().toString(), filesForDay.remove().toURI().toString());
                assertEquals(file4.toURI().toString(), filesForDay.remove().toURI().toString());
            } else if(filesForDay.size() == 4) {
                assertEquals(file5.toURI().toString(), filesForDay.remove().toURI().toString());
                assertEquals(file1.toURI().toString(), filesForDay.remove().toURI().toString());
                assertEquals(file6.toURI().toString(), filesForDay.remove().toURI().toString());
                assertEquals(file8.toURI().toString(), filesForDay.remove().toURI().toString());
            } else if(filesForDay.size() == 1) {
                assertEquals(file7.toURI().toString(), filesForDay.remove().toURI().toString());
            } else throw new IllegalStateException("Wasn't expecting this many files for any day");
        }
        
    }
    
}
