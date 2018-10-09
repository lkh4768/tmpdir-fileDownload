package xyz.swwarehouse.tmpdir.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import xyz.swwarehouse.tmpdir.entity.FileInfo;

public interface FileInfoRepository extends MongoRepository<FileInfo, String> {
	@Query("{id:'?0'}")
	public FileInfo findOne(String id);
}
