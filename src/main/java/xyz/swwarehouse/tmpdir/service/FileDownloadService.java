package xyz.swwarehouse.tmpdir.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import xyz.swwarehouse.tmpdir.entity.FileInfo;
import xyz.swwarehouse.tmpdir.repository.FileInfoRepository;

@Service
public class FileDownloadService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadService.class);

	@Value("${tmpdir.file.root-path}")
	private Path rootPath;

	private final FileInfoRepository fileRepository;
	private final StorageService storageService;

	@Autowired
	public FileDownloadService(FileInfoRepository fileRepository, StorageService storageService) {
		this.fileRepository = fileRepository;
		this.storageService = storageService;
	}

	public FileInfo getFileInfo(String id) {
		FileInfo fileInfo = findFileInfoInRemoteRepo(id);
		LOGGER.debug("Find file info({}) using id({})", fileInfo, id);
		return fileInfo;
	}

	private FileInfo findFileInfoInRemoteRepo(String id) {
		return fileRepository.findOne(id);
	}

	public boolean isExistsFileInfoInRemoteRepo(String id) {
		FileInfo fileinfo = findFileInfoInRemoteRepo(id);
		boolean isExists = fileinfo != null;
		LOGGER.debug("Exists({}) file in remote repo using id({})", isExists, id);
		return isExists;
	}

	public ByteArrayResource getByteArrayResource(final String id) throws IOException {
		Path baseDir = rootPath.resolve(id);
		ByteArrayOutputStream out;
		String filename = "";

		if (storageService.getFileCountInDir(baseDir) == 1) {
			filename = storageService.getFirstFilename(baseDir);
			out = storageService.getFiletoByteArrayOutputStream(baseDir);
		} else {
			filename = id;
			out = storageService.getOutputStreamOfZip(baseDir);
		}

		ByteArrayResource byteArrayResource = createByteArrayResource(out, filename);
		out.close();
		return byteArrayResource;
	}

	private ByteArrayResource createByteArrayResource(final ByteArrayOutputStream out, final String filename) {
		ByteArrayResource byteArrayResource = new ByteArrayResource(out.toByteArray()) {
			@Override
			public String getFilename() {
				try {
					return URLEncoder.encode(filename, StandardCharsets.UTF_8.name());
				} catch (UnsupportedEncodingException e) {
					LOGGER.error("Unsupported Encoding({})", StandardCharsets.UTF_8, e);
				}
				return "";
			}
		};
		LOGGER.debug("Success create byte array resource({})", byteArrayResource);
		return byteArrayResource;
	}

	public boolean isExpiredFile(FileInfo fileInfo) {
		Date curDate = new Date();
		if (fileInfo.getExpireTime() < curDate.getTime()) {
			LOGGER.error("Expired file({}), Current Time({})", fileInfo, curDate);
			return true;
		}
		return false;
	}
}