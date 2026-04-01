package com.example.manifest.controller;

import com.example.manifest.entity.Manifest;
import com.example.manifest.service.ManifestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manifest")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ManifestController {

    private final ManifestService manifestService;

    /** GET /api/manifest — 查询全部货单 */
    @GetMapping
    public ResponseEntity<List<Manifest>> list() {
        return ResponseEntity.ok(manifestService.findAll());
    }

    /** GET /api/manifest/{id} — 根据 ID 查询 */
    @GetMapping("/{id}")
    public ResponseEntity<Manifest> getById(@PathVariable Long id) {
        return manifestService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/manifest — 新增货单 */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody Manifest manifest,
                                                        BindingResult bindingResult) {
        Map<String, Object> result = new HashMap<>();

        if (bindingResult.hasErrors()) {
            result.put("success", false);
            result.put("message", bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }

        if (manifestService.existsByGoodsNo(manifest.getGoodsNo())) {
            result.put("success", false);
            result.put("message", "货品编号已存在，请勿重复添加");
            return ResponseEntity.badRequest().body(result);
        }

        Manifest saved = manifestService.save(manifest);
        result.put("success", true);
        result.put("message", "添加成功");
        result.put("data", saved);
        return ResponseEntity.ok(result);
    }

    /** PUT /api/manifest/{id} — 更新货单 */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                         @Valid @RequestBody Manifest manifest,
                                                         BindingResult bindingResult) {
        Map<String, Object> result = new HashMap<>();

        if (bindingResult.hasErrors()) {
            result.put("success", false);
            result.put("message", bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }

        if (manifestService.findById(id).isEmpty()) {
            result.put("success", false);
            result.put("message", "货单不存在");
            return ResponseEntity.notFound().build();
        }

        manifest.setId(id);
        Manifest updated = manifestService.save(manifest);
        result.put("success", true);
        result.put("message", "更新成功");
        result.put("data", updated);
        return ResponseEntity.ok(result);
    }

    /** DELETE /api/manifest/{id} — 删除货单 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();

        if (manifestService.findById(id).isEmpty()) {
            result.put("success", false);
            result.put("message", "货单不存在");
            return ResponseEntity.notFound().build();
        }

        manifestService.deleteById(id);
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }

    /** GET /api/manifest/goods-no/{goodsNo} — 按货品编号查询 */
    @GetMapping("/goods-no/{goodsNo}")
    public ResponseEntity<Manifest> getByGoodsNo(@PathVariable String goodsNo) {
        return manifestService.findByGoodsNo(goodsNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
