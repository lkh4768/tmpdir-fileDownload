package xyz.swwarehouse.tmpdir.repository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;

import xyz.swwarehouse.tmpdir.entity.FileInfo;

public interface FileInfoRepository extends CrudRepository<FileInfo, String> {
	@Query("{id:'?0'}")
	public FileInfo findOne(String id);
}
