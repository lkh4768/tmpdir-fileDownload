package xyz.swwarehouse.tmpdir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.swwarehouse.tmpdir.entity.FileInfo;
import xyz.swwarehouse.tmpdir.repository.FileInfoRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
public class FileDownloadControllerTests {
	private static boolean INITIALIZED = false;
	private static final int FILE_CNT = 1;
	private static FileInfo[] FILE_INFOS = new FileInfo[FILE_CNT];
	private static FileInfo INVAILD_FILE_INFO;
	private static FileInfo EXPIRED_FILE_INFO;
	private static FileInfoUtil fileInfoUtil;

	@Value("${server.port}")
	private int port;
	@Value("${server.host}")
	private String serverHost;
	@Value("${tmpdir.file.expire-term-day}")
	private int expireTermDay;
	@Value("${tmpdir.file.root-path}")
	private Path rootPath;

	@Autowired
	private WebApplicationContext webApplicationContext;
	private static MockMvc mockMvc = null;

	@Autowired
	private FileInfoRepository fileInfoRepo;

	@Before
	public void setUp() throws IOException {
		if (!INITIALIZED) {
			mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
			fileInfoUtil = new FileInfoUtil(fileInfoRepo, rootPath, expireTermDay);
			FILE_INFOS = fileInfoUtil.createAndSaveFileInfos(FILE_CNT);
			INVAILD_FILE_INFO = fileInfoUtil.createInvaildFileInfo();
			EXPIRED_FILE_INFO = fileInfoUtil.createExpiredFileInfo();
			INITIALIZED = true;
		}
	}

	@Test
	public void testGetFileInfo() throws Exception {
		FileInfo targetFileInfo = FILE_INFOS[0];
		String uri = "/file-info/" + targetFileInfo.getId();

		ObjectMapper objectMapper = new ObjectMapper();
		String jsonResponse = mockMvc.perform(get(uri)).andDo(print()).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
		FileInfo fileInfo = objectMapper.readValue(jsonResponse, FileInfo.class);

		assertNotNull(fileInfo);
		assertEquals(targetFileInfo.getId(), fileInfo.getId());
		assertEquals(targetFileInfo.getSubmissionTime(), fileInfo.getSubmissionTime());
		assertEquals(targetFileInfo.getExpireTime(), fileInfo.getExpireTime());
	}

	@Test
	public void testGetFileInfoNotExists() throws Exception {
		String uri = "/file-info/" + INVAILD_FILE_INFO.getId();
		mockMvc.perform(get(uri)).andDo(print()).andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
	}
	
	@Test
	public void testGetFileInfoExpiredFile() throws Exception {
		String uri = "/file-info/" + EXPIRED_FILE_INFO.getId();
		mockMvc.perform(get(uri)).andDo(print()).andExpect(status().is(HttpStatus.PRECONDITION_FAILED.value()));
	}

	@Test
	public void testDownloadFile() throws Exception {
		FileInfo targetFileInfo = FILE_INFOS[0];
		String uri = "/file/" + targetFileInfo.getId();
		MockHttpServletResponse response = mockMvc.perform(get(uri)).andDo(print()).andExpect(status().isOk()).andReturn()
				.getResponse();
		String disposition = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);
		String filename = disposition.split("=")[1];
		filename = filename.replaceAll("\"", "");
		String filecontent = URLDecoder.decode(response.getContentAsString(), StandardCharsets.UTF_8.name());

		ByteArrayResource expectedByteArrayResource = fileInfoUtil.getByteArrayResource(targetFileInfo.getId());
		assertEquals(expectedByteArrayResource.getFilename(), filename);
		assertEquals(new String(expectedByteArrayResource.getByteArray()), filecontent);
	}

	@Test
	public void testDownloadFileNotExist() throws Exception {
		String uri = "/file/" + INVAILD_FILE_INFO.getId();
		mockMvc.perform(get(uri)).andDo(print()).andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
	}
}
