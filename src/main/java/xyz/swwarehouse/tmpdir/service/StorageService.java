package xyz.swwarehouse.tmpdir.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StorageService {
	private static final Logger LOGGER = LoggerFactory.getLogger(StorageService.class);

	public int getFileCountInDir(final Path dir) throws NoSuchFileException {
		File[] files = new File(dir.toString()).listFiles();
		if (files == null) {
			LOGGER.error("Not found directory({})", dir);
			throw new NoSuchFileException(dir.toString());
		}
		int count = files.length;
		LOGGER.debug("File count({}) in directory({})", count, dir);
		return count;
	}

	public String getFirstFilename(final Path dir) throws IOException {
		ArrayList<Path> filePaths = getFilePathsInDir(dir);
		Path firstPath = filePaths.get(0).getFileName();
		String firstFilename = "";
		if (firstPath != null)
			firstFilename = firstPath.toString();
		else
			LOGGER.warn("First path in directory({}) is null", dir);
		LOGGER.debug("First filename({}) in directory({}) ", firstFilename, dir);
		return firstFilename;
	}

	private ArrayList<Path> getFilePathsInDir(final Path dir) throws IOException {
		ArrayList<Path> filePaths = new ArrayList<>();
		DirectoryStream<Path> directoryStream = null;
		try {
			directoryStream = Files.newDirectoryStream(dir);
			for (Path path : directoryStream)
				filePaths.add(path);
		} catch (IOException e) {
			LOGGER.error("Failed new directory stream of directory({})", dir, e);
			throw new IOException(e.getMessage(), e.getCause());
		} finally {
			if (directoryStream != null)
				directoryStream.close();
		}
		LOGGER.debug("Path({}) of sub files in directory({})", filePaths, dir);
		return filePaths;
	}

	public ByteArrayOutputStream getFiletoByteArrayOutputStream(final Path dir) throws IOException {
		File file = getFirstFile(dir);
		return getFiletoByteArrayOutputStream(file);
	}

	private File getFirstFile(final Path dir) throws IOException {
		ArrayList<Path> filePaths = getFilePathsInDir(dir);
		return filePaths.get(0).toFile();
	}

	private ByteArrayOutputStream getFiletoByteArrayOutputStream(final File file) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RandomAccessFile aFile = null;
		try {
			aFile = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException e) {
			LOGGER.error("Failed creating random access file({})", file, e);
			throw e;
		}

		FileChannel inChannel = aFile.getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		try {
			while (inChannel.read(buffer) > 0) {
				buffer.flip();
				out.write(buffer.array());
				buffer.clear();
			}
		} catch (IOException e) {
			LOGGER.error("Failed reading file({}) using channel({})", aFile, inChannel, e);
			throw new IOException(e.getMessage(), e.getCause());
		} finally {
			inChannel.close();
			aFile.close();
		}
		LOGGER.debug("Success get output stream({}) of file({})", out, file);
		return out;
	}

	public ByteArrayOutputStream getOutputStreamOfZip(final Path dir) throws IOException {
		ArrayList<Path> filePaths = getFilePathsInDir(dir);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int filePathsSize = filePaths.size();
		ZipOutputStream zos = new ZipOutputStream(out);
		for (int i = 0; i < filePathsSize; i++) {
			Path filePath = filePaths.get(i);
			Path filenamePath = filePath.getFileName();
			String filename = "";
			if (filenamePath != null)
				filename = filenamePath.toString();
			ByteArrayOutputStream fileBaos = new ByteArrayOutputStream();
			fileBaos = getFiletoByteArrayOutputStream(filePath.toFile());
			ZipEntry zipEntry = new ZipEntry(filename);
			zipEntry.setSize(fileBaos.size());
			try {
				zos.putNextEntry(zipEntry);
				zos.write(fileBaos.toByteArray());
			} catch (IOException e) {
				LOGGER.debug("Failed writing byte({}) or putNextEntry entry({}) zipOutputStream({})", fileBaos.toByteArray(),
						zipEntry, out, e);
				throw new IOException(e.getMessage(), e.getCause());
			} finally {
				try {
					zos.closeEntry();
					fileBaos.close();
				} catch (IOException e) {
					LOGGER.debug("Failed closing zipOutputStream({}) or OutputStream({})", zos, fileBaos, e);
					throw new IOException(e.getMessage(), e.getCause());
				}
			}
		}
		zos.close();
		LOGGER.debug("Success output stream({}) of zip", out);
		return out;
	}
}
