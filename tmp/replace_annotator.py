import sys

with open('app/src/main/java/com/example/ui/screens/PdfAppScreens.kt', 'r') as f:
    content = f.read()

target = """                                 Row(
                                     horizontalArrangement = Arrangement.spacedBy(8.dp),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Button(
                                         onClick = { 
                                             addWordX = 0.35f
                                             addWordY = 0.35f
                                             addWordTextDraft = ""
                                             showAddWordDialog = true
                                         },
                                         shape = RoundedCornerShape(16.dp),
                                         contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                         colors = ButtonDefaults.buttonColors(
                                             containerColor = Color(0xFFDBEAFE),
                                             contentColor = Color(0xFF2563EB)
                                         ),
                                         modifier = Modifier.height(32.dp)
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.Add,
                                             contentDescription = "Add Text Box",
                                             modifier = Modifier.size(16.dp)
                                         )
                                         Spacer(modifier = Modifier.width(4.dp))
                                         Text("+ Add Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                     }

                                     val selectedAnn = currentTextAnns.find { it.id == selectedAnnotationId }
                                     Button(
                                         onClick = { 
                                             if (selectedAnn != null) {
                                                 selectedWordToEdit = selectedAnn
                                                 editWordTextDraft = selectedAnn.text
                                                 editWordFontSize = selectedAnn.fontSize
                                                 editWordColorHex = selectedAnn.colorHex
                                                 editWordFontFamily = selectedAnn.fontName
                                                 editWordBgColorHex = selectedAnn.bgColorHex
                                                 editWordHasOutline = selectedAnn.hasOutline
                                                 editWordHasUnderline = selectedAnn.hasUnderline
                                                 editWordOutlineColorHex = selectedAnn.outlineColorHex
                                                 editWordIsBold = selectedAnn.isBold
                                                 editWordAlignment = selectedAnn.alignment
                                                 editWordIsPowerOf = selectedAnn.isPowerOf
                                                 editWordIsItalic = selectedAnn.isItalic
                                                 editWordHasStrikeThrough = selectedAnn.hasStrikeThrough
                                                 showEditWordDialog = true
                                             }
                                         },
                                         enabled = selectedAnn != null,
                                         shape = RoundedCornerShape(16.dp),
                                         contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                         colors = ButtonDefaults.buttonColors(
                                             containerColor = if (selectedAnn != null) Color(0xFF2563EB) else Color.Transparent,
                                             contentColor = if (selectedAnn != null) Color.White else Color.Gray,
                                             disabledContainerColor = Color.Transparent,
                                             disabledContentColor = Color.Gray
                                         ),
                                         modifier = Modifier.height(32.dp)
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.Edit,
                                             contentDescription = "Edit Text",
                                             modifier = Modifier.size(16.dp)
                                         )
                                         Spacer(modifier = Modifier.width(4.dp))
                                         Text("Edit Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                     }

                                     Spacer(modifier = Modifier.width(4.dp))

                                     Button(
                                         onClick = { 
                                             selectedAnn?.let { ann ->
                                                 val duplicated = ann.copy(
                                                     id = "word_${System.currentTimeMillis()}",
                                                     x = (ann.x + 0.03f).coerceIn(0f, 0.9f),
                                                     y = (ann.y + 0.03f).coerceIn(0f, 0.9f)
                                                 )
                                                 currentTextAnns.add(duplicated)
                                                 viewModel.editActivePageAnnotations(currentTextAnns.toList())
                                                 selectedAnnotationId = duplicated.id
                                             }
                                         },
                                         enabled = selectedAnn != null,
                                         shape = RoundedCornerShape(16.dp),
                                         contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                         colors = ButtonDefaults.buttonColors(
                                             containerColor = if (selectedAnn != null) Color(0xFF10B981) else Color.Transparent,
                                             contentColor = if (selectedAnn != null) Color.White else Color.Gray,
                                             disabledContainerColor = Color.Transparent,
                                             disabledContentColor = Color.Gray
                                         ),
                                         modifier = Modifier.height(32.dp)
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.ContentCopy,
                                             contentDescription = "Duplicate Text",
                                             modifier = Modifier.size(16.dp)
                                         )
                                         Spacer(modifier = Modifier.width(4.dp))
                                         Text("Duplicate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                     }
                                }"""

replacement = """                                 Row(
                                     horizontalArrangement = Arrangement.spacedBy(8.dp),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     IconButton(
                                         onClick = { 
                                             addWordX = 0.35f
                                             addWordY = 0.35f
                                             addWordTextDraft = ""
                                             showAddWordDialog = true
                                         },
                                         modifier = Modifier
                                             .size(36.dp)
                                             .background(Color(0xFFDBEAFE), CircleShape)
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.Add,
                                             contentDescription = "Add Text Box",
                                             tint = Color(0xFF2563EB),
                                             modifier = Modifier.size(20.dp)
                                         )
                                     }

                                     val selectedAnn = currentTextAnns.find { it.id == selectedAnnotationId }
                                     IconButton(
                                         onClick = { 
                                             if (selectedAnn != null) {
                                                 selectedWordToEdit = selectedAnn
                                                 editWordTextDraft = selectedAnn.text
                                                 editWordFontSize = selectedAnn.fontSize
                                                 editWordColorHex = selectedAnn.colorHex
                                                 editWordFontFamily = selectedAnn.fontName
                                                 editWordBgColorHex = selectedAnn.bgColorHex
                                                 editWordHasOutline = selectedAnn.hasOutline
                                                 editWordHasUnderline = selectedAnn.hasUnderline
                                                 editWordOutlineColorHex = selectedAnn.outlineColorHex
                                                 editWordIsBold = selectedAnn.isBold
                                                 editWordAlignment = selectedAnn.alignment
                                                 editWordIsPowerOf = selectedAnn.isPowerOf
                                                 editWordIsItalic = selectedAnn.isItalic
                                                 editWordHasStrikeThrough = selectedAnn.hasStrikeThrough
                                                 showEditWordDialog = true
                                             }
                                         },
                                         enabled = selectedAnn != null,
                                         modifier = Modifier
                                             .size(36.dp)
                                             .background(if (selectedAnn != null) Color(0xFFDBEAFE) else Color(0xFFF1F5F9), CircleShape)
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.Edit,
                                             contentDescription = "Edit Text",
                                             tint = if (selectedAnn != null) Color(0xFF2563EB) else Color(0xFF94A3B8),
                                             modifier = Modifier.size(18.dp)
                                         )
                                     }

                                     IconButton(
                                         onClick = { 
                                             selectedAnn?.let { ann ->
                                                 val duplicated = ann.copy(
                                                     id = "word_${System.currentTimeMillis()}",
                                                     x = (ann.x + 0.03f).coerceIn(0f, 0.9f),
                                                     y = (ann.y + 0.03f).coerceIn(0f, 0.9f)
                                                 )
                                                 currentTextAnns.add(duplicated)
                                                 viewModel.editActivePageAnnotations(currentTextAnns.toList())
                                                 selectedAnnotationId = duplicated.id
                                             }
                                         },
                                         enabled = selectedAnn != null,
                                         modifier = Modifier
                                             .size(36.dp)
                                             .background(if (selectedAnn != null) Color(0xFFD1FAE5) else Color(0xFFF1F5F9), CircleShape)
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.ContentCopy,
                                             contentDescription = "Duplicate Text",
                                             tint = if (selectedAnn != null) Color(0xFF059669) else Color(0xFF94A3B8),
                                             modifier = Modifier.size(18.dp)
                                         )
                                     }

                                     IconButton(
                                         onClick = { 
                                             selectedAnn?.let { ann ->
                                                 val newSize = (ann.fontSize - 1f).coerceAtLeast(6f)
                                                 val updated = ann.copy(fontSize = newSize)
                                                 val idx = currentTextAnns.indexOfFirst { it.id == ann.id }
                                                 if (idx != -1) {
                                                     currentTextAnns[idx] = updated
                                                     viewModel.editActivePageAnnotations(currentTextAnns.toList())
                                                 }
                                             }
                                         },
                                         enabled = selectedAnn != null,
                                         modifier = Modifier
                                             .size(36.dp)
                                             .background(if (selectedAnn != null) Color(0xFFFEF3C7) else Color(0xFFF1F5F9), CircleShape)
                                     ) {
                                         Text(
                                             text = "A",
                                             fontSize = 11.sp,
                                             fontWeight = FontWeight.Bold,
                                             color = if (selectedAnn != null) Color(0xFFD97706) else Color(0xFF94A3B8)
                                         )
                                     }

                                     IconButton(
                                         onClick = { 
                                             selectedAnn?.let { ann ->
                                                 val newSize = (ann.fontSize + 1f).coerceAtMost(100f)
                                                 val updated = ann.copy(fontSize = newSize)
                                                 val idx = currentTextAnns.indexOfFirst { it.id == ann.id }
                                                 if (idx != -1) {
                                                     currentTextAnns[idx] = updated
                                                     viewModel.editActivePageAnnotations(currentTextAnns.toList())
                                                 }
                                             }
                                         },
                                         enabled = selectedAnn != null,
                                         modifier = Modifier
                                             .size(36.dp)
                                             .background(if (selectedAnn != null) Color(0xFFFEF3C7) else Color(0xFFF1F5F9), CircleShape)
                                     ) {
                                         Text(
                                             text = "A",
                                             fontSize = 17.sp,
                                             fontWeight = FontWeight.Bold,
                                             color = if (selectedAnn != null) Color(0xFFD97706) else Color(0xFF94A3B8)
                                         )
                                     }
                                 }"""

lines_content = content.split('\n')
lines_target = target.split('\n')

match_idx = -1
for i in range(len(lines_content) - len(lines_target) + 1):
    found = True
    for j in range(len(lines_target)):
        if lines_content[i+j].rstrip() != lines_target[j].rstrip():
            found = False
            break
    if found:
        match_idx = i
        break

if match_idx != -1:
    print('Found target at line', match_idx + 1)
    new_lines = lines_content[:match_idx] + replacement.split('\n') + lines_content[match_idx + len(lines_target):]
    with open('app/src/main/java/com/example/ui/screens/PdfAppScreens.kt', 'w') as f:
        f.write('\n'.join(new_lines))
    print('SUCCESS')
else:
    print('FAIL: Target content not matched')
    sys.exit(1)
