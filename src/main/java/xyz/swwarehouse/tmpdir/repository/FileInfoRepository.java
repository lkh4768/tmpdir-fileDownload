package xyz.swwarehouse.tmpdir.repository;

import org.springframework.data.repository.CrudRepository;

import xyz.swwarehouse.tmpdir.entity.FileInfo;

public interface FileInfoRepository extends CrudRepository<FileInfo, String> {
}
