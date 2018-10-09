package xyz.swwarehouse.tmpdir;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import xyz.swwarehouse.tmpdir.entity.FileInfo;
import xyz.swwarehouse.tmpdir.repository.FileInfoRepository;
import xyz.swwarehouse.tmpdir.service.FileDownloadService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
public class FileDownloadServiceTests {
	private static boolean INITIALIZED = false;
	private static int FILE_CNT = 2;
	private static FileInfo[] FILE_INFOS = new FileInfo[FILE_CNT];
	private static FileInfo INVAILD_FILE_INFO;
	private static FileInfo EXPIRED_FILE_INFO;
	private static FileInfoUtil fileInfoUtil;

	@Value("${tmpdir.file.expire-term-day}")
	private int expireTermDay;
	@Value("${tmpdir.file.root-path}")
	private Path rootPath;

	@Autowired
	private FileDownloadService fileDownloadService;
	@Autowired
	private FileInfoRepository fileInfoRepo;

	@Before
	public void setUp() throws Exception {
		if (!INITIALIZED) {
			fileInfoUtil = new FileInfoUtil(fileInfoRepo, rootPath, expireTermDay);
			FILE_INFOS = fileInfoUtil.createAndSaveFileInfos(FILE_CNT);
			INVAILD_FILE_INFO = fileInfoUtil.createInvaildFileInfo();
			EXPIRED_FILE_INFO = fileInfoUtil.createExpiredFileInfo();
			INITIALIZED = true;
		}
	}

	@Test
	public void testGetFileInfo() {
		FileInfo targetFileInfo = FILE_INFOS[0];
		FileInfo fileInfo = fileDownloadService.getFileInfo(targetFileInfo.getId());

		assertNotNull(fileInfo);
		assertEquals(targetFileInfo.getId(), fileInfo.getId());
		assertEquals(targetFileInfo.getSubmissionTime(), fileInfo.getSubmissionTime());
		assertEquals(targetFileInfo.getExpireTime(), fileInfo.getExpireTime());
	}

	@Test
	public void testGetFileInfoNotExists() {
		FileInfo fileInfo = fileDownloadService.getFileInfo(INVAILD_FILE_INFO.getId());
		assertNull(fileInfo);
	}

	@Test
	public void testIsExistsFileInfoInRemoteRepo() {
		FileInfo targetFileInfo = FILE_INFOS[0];
		assertTrue(fileDownloadService.isExistsFileInfoInRemoteRepo(targetFileInfo.getId()));
	}

	@Test
	public void testIsExistsFileInfoInRemoteRepoNotExists() {
		assertFalse(fileDownloadService.isExistsFileInfoInRemoteRepo(INVAILD_FILE_INFO.getId()));
	}

	@Test
	public void testGetFilesSingleFile() throws IOException {
		FileInfo targetFileInfo = FILE_INFOS[0];
		ByteArrayResource byteArrayResource = fileDownloadService.getByteArrayResource(targetFileInfo.getId());
		ByteArrayResource expectedByteArrayResource = fileInfoUtil.getByteArrayResource(targetFileInfo.getId());

		assertEquals(expectedByteArrayResource.getFilename(), byteArrayResource.getFilename());
		assertArrayEquals(expectedByteArrayResource.getByteArray(), byteArrayResource.getByteArray());
	}

	@Test
	public void testGetFilesMultiFile() throws IOException {
		FileInfo targetFileInfo = FILE_INFOS[1];
		ByteArrayResource byteArrayResource = fileDownloadService.getByteArrayResource(targetFileInfo.getId());
		ByteArrayResource expectedByteArrayResource = fileInfoUtil.getByteArrayResource(targetFileInfo.getId());

		assertEquals(expectedByteArrayResource.getFilename(), byteArrayResource.getFilename());
		assertArrayEquals(expectedByteArrayResource.getByteArray(), byteArrayResource.getByteArray());
	}

	@Test(expected = IOException.class)
	public void testGetFilesNotExists() throws IOException, NotFoundException {
		ByteArrayResource byteArrayResource = fileDownloadService.getByteArrayResource(INVAILD_FILE_INFO.getId());
		assertNull(byteArrayResource);
	}

	@Test
	public void testIsExpiredFile() {
		FileInfo targetFileInfo = FILE_INFOS[0];
		assertFalse(fileDownloadService.isExpiredFile(targetFileInfo));
	}

	@Test
	public void testIsExpiredFileExpired() {
		assertTrue(fileDownloadService.isExpiredFile(EXPIRED_FILE_INFO));
	}
}
