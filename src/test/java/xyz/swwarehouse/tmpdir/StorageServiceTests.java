package xyz.swwarehouse.tmpdir;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import xyz.swwarehouse.tmpdir.entity.FileInfo;
import xyz.swwarehouse.tmpdir.repository.FileInfoRepository;
import xyz.swwarehouse.tmpdir.service.StorageService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
public class StorageServiceTests {
	private static boolean INITIALIZED = false;
	private static int FILE_CNT = 2;
	private static FileInfo[] FILE_INFOS = new FileInfo[FILE_CNT];
	private static FileInfo INVAILD_FILE_INFO;
	private static FileInfoUtil fileInfoUtil;

	@Autowired
	private StorageService storageService;

	@Autowired
	private FileInfoRepository fileInfoRepo;

	@Value("${tmpdir.file.expire-term-day}")
	private int expireTermDay;
	@Value("${tmpdir.file.root-path}")
	private Path rootPath;

	@Before
	public void setUp() throws Exception {
		if (!INITIALIZED) {
			fileInfoUtil = new FileInfoUtil(fileInfoRepo, rootPath, expireTermDay);
			FILE_INFOS = fileInfoUtil.createAndSaveFileInfos(FILE_CNT);
			INVAILD_FILE_INFO = fileInfoUtil.createInvaildFileInfo();
			INITIALIZED = true;
		}
	}

	@Test
	public void testGetFileCountInDir() throws IOException {
		FileInfo fileInfo0 = FILE_INFOS[0];
		FileInfo fileInfo1 = FILE_INFOS[1];
		assertEquals(storageService.getFileCountInDir(rootPath.resolve(fileInfo0.getId())), 1);
		assertEquals(storageService.getFileCountInDir(rootPath.resolve(fileInfo1.getId())), 2);
	}
	
	@Test(expected = NoSuchFileException.class)
	public void testGetFileCountInDirNotExist() throws IOException, NotFoundException {
		storageService.getFileCountInDir(rootPath.resolve(INVAILD_FILE_INFO.getId()));
	}

	@Test
	public void testGetFirstFilename() throws IOException{
		FileInfo fileInfo0 = FILE_INFOS[0];
		assertEquals(storageService.getFirstFilename(rootPath.resolve(fileInfo0.getId())), fileInfo0.getId()+"0");
	}

	@Test(expected = IOException.class)
	public void testGetFirstFilenameNotExist() throws IOException, NotFoundException {
		storageService.getFirstFilename(rootPath.resolve(INVAILD_FILE_INFO.getId()));
	}

	@Test
	public void testGetFiletoByteArrayOutputStream() throws IOException {
		FileInfo targetFileInfo = FILE_INFOS[0];
		Path baseDir = rootPath.resolve(targetFileInfo.getId());
		ByteArrayOutputStream out = storageService.getFiletoByteArrayOutputStream(baseDir);
		ByteArrayOutputStream expectedOut = fileInfoUtil.getFiletoByteArrayOutputStream(baseDir);
		
		assertArrayEquals(expectedOut.toByteArray(), out.toByteArray());
	}
	
	@Test(expected = IOException.class)
	public void testGetFiletoByteArrayOutputStreamNotExist() throws IOException, NotFoundException {
		Path baseDir = rootPath.resolve(INVAILD_FILE_INFO.getId());
		storageService.getFiletoByteArrayOutputStream(baseDir);
	}

	@Test
	public void testGetOutputStreamOfZip() throws IOException {
		FileInfo targetFileInfo = FILE_INFOS[1];
		Path baseDir = rootPath.resolve(targetFileInfo.getId());
		ByteArrayOutputStream out = storageService.getOutputStreamOfZip(baseDir);
		ByteArrayOutputStream expectedOut = fileInfoUtil.getOutputStreamOfZip(baseDir);
		
		assertArrayEquals(expectedOut.toByteArray(), out.toByteArray());
	}

	@Test(expected = IOException.class)
	public void testGetOutputStreamOfZipNotExist() throws IOException, NotFoundException {
		Path baseDir = rootPath.resolve(INVAILD_FILE_INFO.getId());
		storageService.getOutputStreamOfZip(baseDir);
	}
}
