package ru.rtc.warehouse.common;

import java.util.List;

public interface CrudEntityService<T, ID> {
	T save(T t);
	T update(T t);
	List<T> findAll();
	T findById(ID id);
	void delete(ID id);
}
