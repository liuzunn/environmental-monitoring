# -*- coding: utf-8 -*-
"""渲染 PDF 关键页为 PNG 图片，供可视化核对。"""
import fitz

pdf = fitz.open('preview.pdf')
print('总页数:', pdf.page_count)
# 渲染封面、摘要、目录、正文首章、含表格章节、参考文献页
targets = [0, 1, 2, 3, 4, 5, 6, 7]
for i in targets:
    if i < pdf.page_count:
        page = pdf[i]
        pix = page.get_pixmap(dpi=100)
        pix.save('page_%02d.png' % (i + 1))
        print('已渲染 page_%02d.png' % (i + 1))
