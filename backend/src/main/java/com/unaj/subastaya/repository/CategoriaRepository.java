package com.unaj.subastaya.repository;

import com.unaj.subastaya.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
