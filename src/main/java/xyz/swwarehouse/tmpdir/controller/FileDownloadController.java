package xyz.swwarehouse.tmpdir.controller;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import xyz.swwarehouse.tmpdir.entity.FileInfo;
import xyz.swwarehouse.tmpdir.service.ExpiredFileException;
import xyz.swwarehouse.tmpdir.service.FileDownloadService;
import xyz.swwarehouse.tmpdir.service.NotFoundIDException;
import xyz.swwarehouse.tmpdir.util.Util;

@RestController
public class FileDownloadController {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadController.class);
	FileDownloadService fileDownloadService;

	@Autowired
	public FileDownloadController(FileDownloadService fileUploadService) {
		this.fileDownloadService = fileUploadService;
	}

	@RequestMapping(value = "/file-info/{id}", method = RequestMethod.GET)
	public ResponseEntity<FileInfo> getFileInfo(HttpServletRequest req, @PathVariable String id) throws Exception {
		LOGGER.info("{}", Util.requestInfoToString(req));
		FileInfo fileInfo = fileDownloadService.getFileInfo(id);
		if (fileInfo == null) {
			LOGGER.info("Not found fileInfo using id({})", id);
			throw new NotFoundIDException("Not found id");
		}
		if (fileDownloadService.isExpiredFile(fileInfo))
			throw new ExpiredFileException("Expired file");
		LOGGER.info("Response ({}->{}), fileInfo({})", Util.getLocalInfo(req), Util.getClientInfo(req), fileInfo);
		return new ResponseEntity<>(fileInfo, HttpStatus.OK);
	}

	@RequestMapping(value = "/file/{id}", method = RequestMethod.GET)
	public ResponseEntity<Resource> downloadFile(HttpServletRequest req, @PathVariable String id) throws Exception {
		LOGGER.info("{}", Util.requestInfoToString(req));
		if (!fileDownloadService.isExistsFileInfoInRemoteRepo(id)) {
			LOGGER.info("Not found fileInfo using id({})", id);
			throw new NotFoundIDException("Not found file info using id");
		}

		ByteArrayResource byteArrayResource = fileDownloadService.getByteArrayResource(id);
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + byteArrayResource.getFilename() + "\"");
		LOGGER.info("Response ({}->{}), download file({})", Util.getLocalInfo(req), Util.getClientInfo(req),
				byteArrayResource.getFilename());
		return ResponseEntity.ok().headers(headers).contentLength(byteArrayResource.contentLength())
				.contentType(MediaType.APPLICATION_OCTET_STREAM).body(byteArrayResource);
	}
}
