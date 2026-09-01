# -*- coding: utf-8 -*-
import re, io
text = io.open('paper_content.py', encoding='utf-8').read()
bad_words = ['深入探讨', '至关重要', '需要注意的是', '综上所述', '总而言之', '众所周知',
             '值得一提的是', '不可忽视', '毋庸置疑', '总的来说']
hit = False
for w in bad_words:
    n = text.count(w)
    if n:
        print('命中AI味词:', w, '次数:', n)
        hit = True
if not hit:
    print('未命中常见 AI 味词')
print('破折号(——)出现次数:', text.count('——'))
# 检查中文语境下是否误用半角引号
m = re.findall(r'[\u4e00-\u9fff]["\'][\u4e00-\u9fff]', text)
print('可疑半角引号:', m[:5] if m else '无')
# 检查“。”是否统一、是否有英文句点夹在中文中
m2 = re.findall(r'[\u4e00-\u9fff]\.[\u4e00-\u9fff]', text)
print('中文中夹英文句点:', m2[:8] if m2 else '无')
# 段落长度节奏检查（正文段字数）
import ast
print('扫描完成')
