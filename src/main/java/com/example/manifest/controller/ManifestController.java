package com.example.manifest.controller;

import com.example.manifest.entity.Manifest;
import com.example.manifest.service.ManifestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    /** 获取所有货单（不含明细，用于列表展示） */
    @GetMapping
    public ResponseEntity<List<Manifest>> list() {
        List<Manifest> list = manifestService.findAll();
        // 明细已在 EAGER fetch 中，一起返回
        return ResponseEntity.ok(list);
    }

    /** 获取单个货单（包含明细） */
    @GetMapping("/{id}")
    public ResponseEntity<Manifest> getById(@PathVariable Integer id) {
        Manifest m = manifestService.findById(id);
        if (m == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(m);
    }

    /** 新增货单 */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Manifest manifest) {
        Map<String, Object> result = new HashMap<>();
        if (manifest.getCustomerId() == null) {
            result.put("success", false);
            result.put("message", "请选择客户");
            return ResponseEntity.badRequest().body(result);
        }
        if (manifest.getItems() == null || manifest.getItems().isEmpty()) {
            result.put("success", false);
            result.put("message", "请至少添加一个产品明细");
            return ResponseEntity.badRequest().body(result);
        }
        Manifest saved = manifestService.save(manifest);
        result.put("success", true);
        result.put("message", "货单创建成功");
        result.put("data", saved);
        return ResponseEntity.ok(result);
    }

    /** 更新货单 */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id,
                                                       @RequestBody Manifest manifest) {
        Map<String, Object> result = new HashMap<>();
        Manifest existing = manifestService.findById(id);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "货单不存在");
            return ResponseEntity.notFound().build();
        }
        if (manifest.getItems() == null || manifest.getItems().isEmpty()) {
            result.put("success", false);
            result.put("message", "请至少添加一个产品明细");
            return ResponseEntity.badRequest().body(result);
        }
        manifest.setId(id);
        Manifest saved = manifestService.save(manifest);
        result.put("success", true);
        result.put("message", "货单更新成功");
        result.put("data", saved);
        return ResponseEntity.ok(result);
    }

    /** 删除货单 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        Manifest existing = manifestService.findById(id);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "货单不存在");
            return ResponseEntity.notFound().build();
        }
        manifestService.deleteById(id);
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }

    /** 搜索货单（按客户名或手机号） */
    @GetMapping("/search")
    public ResponseEntity<List<Manifest>> search(@RequestParam String kw) {
        return ResponseEntity.ok(manifestService.findAll().stream()
                .filter(m -> (m.getCustomerName() != null && m.getCustomerName().contains(kw))
                          || (m.getCustomerPhone() != null && m.getCustomerPhone().contains(kw)))
                .toList());
    }
}
