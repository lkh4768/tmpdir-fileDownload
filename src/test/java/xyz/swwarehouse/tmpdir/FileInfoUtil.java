package xyz.swwarehouse.tmpdir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.core.io.ByteArrayResource;

import xyz.swwarehouse.tmpdir.entity.FileInfo;
import xyz.swwarehouse.tmpdir.repository.FileInfoRepository;

public class FileInfoUtil {
	private int expireTermDay;
	private Path rootPath;
	private FileInfoRepository fileInfoRepo;

	public FileInfoUtil(FileInfoRepository fileInfoRepo, Path rootPath, int expireTermDay) {
		this.fileInfoRepo = fileInfoRepo;
		this.rootPath = rootPath;
		this.expireTermDay = expireTermDay;
	}

	public FileInfo createInvaildFileInfo() {
		return createFileInfo();
	}
	
	public FileInfo createExpiredFileInfo() throws IOException {
		FileInfo fileInfo = createFileInfo(createExpiredSubmissionTime());
		storeFile(fileInfo, 0);
		fileInfoRepo.save(fileInfo);
		return fileInfo;
	}

	public FileInfo[] createAndSaveFileInfos(final int cnt) throws IOException {
		FileInfo[] fileInfos = new FileInfo[cnt];
		for (int i = 0; i < cnt; i++) {
			fileInfos[i] = createAndSaveFileInfo(i + 1);
			System.out.println("createAndSaveFileInfo{id: " + fileInfos[i].getId() + ", submissionTime: "
					+ fileInfos[i].getSubmissionTime() + ", expireTime: " + fileInfos[i].getExpireTime() + "}");
		}
		return fileInfos;
	}

	private FileInfo createAndSaveFileInfo(final int fileCount) throws IOException {
		FileInfo fileInfo = createFileInfo();
		for (int i = 0; i < fileCount; i++)
			storeFile(fileInfo, i);
		fileInfoRepo.save(fileInfo);
		return fileInfo;
	}

	private FileInfo createFileInfo() {
		FileInfo fileInfo = new FileInfo();
		fileInfo.setId(createId());
		fileInfo.setSubmissionTime(createSubmissionTime());
		fileInfo.setExpireTime(createExpireTime(fileInfo));
		return fileInfo;
	}
	
	private FileInfo createFileInfo(long submissionTime) {
		FileInfo fileInfo = new FileInfo();
		fileInfo.setId(createId());
		fileInfo.setSubmissionTime(submissionTime);
		fileInfo.setExpireTime(createExpireTime(fileInfo));
		return fileInfo;
	}

	private String createId() {
		return UUID.randomUUID().toString();
	}

	private long createSubmissionTime() {
		return new Date().getTime();
	}
	
	private long createExpiredSubmissionTime() {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, -(expireTermDay+1));
		return c.getTime().getTime();
	}

	private long createExpireTime(FileInfo fileInfo) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date(fileInfo.getSubmissionTime()));
		c.add(Calendar.DATE, expireTermDay);
		return c.getTime().getTime();
	}

	private void storeFile(FileInfo fileInfo, int idx) throws IOException {
		Path baseDirPath = createDir(fileInfo.getId());
		InputStream filecontent = new ByteArrayInputStream(fileInfo.getId().getBytes(StandardCharsets.UTF_8.name()));
		Path filePath = baseDirPath.resolve(fileInfo.getId() + idx);
		if (!filePath.toFile().exists()) {
			Files.copy(filecontent, filePath, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("storeFile(filename: " + fileInfo.getId() + idx + ")");
		}
	}

	private Path createDir(final String id) throws IOException {
		Path storeFilePath = this.rootPath.resolve(id);
		if (!this.rootPath.toFile().exists())
			Files.createDirectory(this.rootPath);
		if (!storeFilePath.toFile().exists())
			Files.createDirectory(storeFilePath);
		return storeFilePath;
	}

	public ByteArrayResource getByteArrayResource(final String id) throws IOException {
		Path baseDir = rootPath.resolve(id);
		ArrayList<Path> filePaths = getFilePathsInDir(baseDir);
		int filePathsSize = filePaths.size();
		ByteArrayOutputStream out;
		String filename = "";

		if (filePathsSize == 1) {
			filename = filePaths.get(0).getFileName().toString();
			out = getFiletoByteArrayOutputStream(filePaths.get(0).toFile());
		} else {
			filename = id;
			out = getOutputStreamOfZip(filePaths);
		}

		return createByteArrayResource(out, filename);
	}

	private ArrayList<Path> getFilePathsInDir(final Path dir) throws IOException {
		ArrayList<Path> filePaths = new ArrayList<>();
		DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir);
		for (Path path : directoryStream)
			filePaths.add(path);
		return filePaths;
	}

	public ByteArrayOutputStream getFiletoByteArrayOutputStream(final Path dir) throws IOException {
		return getFiletoByteArrayOutputStream(getFilePathsInDir(dir).get(0).toFile());
	}

	public ByteArrayOutputStream getFiletoByteArrayOutputStream(final File file) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RandomAccessFile aFile = new RandomAccessFile(file, "r");
		FileChannel inChannel = aFile.getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		while (inChannel.read(buffer) > 0) {
			buffer.flip();
			out.write(buffer.array());
			buffer.clear();
		}
		inChannel.close();
		aFile.close();
		return out;
	}

	public ByteArrayOutputStream getOutputStreamOfZip(final Path dir) throws IOException {
		return getOutputStreamOfZip(getFilePathsInDir(dir));
	}

	private ByteArrayOutputStream getOutputStreamOfZip(ArrayList<Path> filePaths) throws IOException {
		int filePathsSize = filePaths.size();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(out);
		for (int i = 0; i < filePathsSize; i++) {
			Path filePath = filePaths.get(i);
			String filename = filePath.getFileName().toString();
			ByteArrayOutputStream fileBaos = new ByteArrayOutputStream();
			fileBaos = getFiletoByteArrayOutputStream(filePath.toFile());
			ZipEntry zipEntry = new ZipEntry(filename);
			zipEntry.setSize(fileBaos.size());
			zos.putNextEntry(zipEntry);
			zos.write(fileBaos.toByteArray());
			zos.closeEntry();
			fileBaos.close();
		}
		zos.close();
		return out;
	}

	private ByteArrayResource createByteArrayResource(final ByteArrayOutputStream out, final String filename) {
		ByteArrayResource byteArrayResource = new ByteArrayResource(out.toByteArray()) {
			@Override
			public String getFilename() {
				try {
					return URLEncoder.encode(filename, StandardCharsets.UTF_8.name());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return "";
			}
		};
		return byteArrayResource;
	}
}
