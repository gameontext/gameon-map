{
  "map": "function(doc) {\n  if ( doc.coord && doc.type == \"empty\" ) {\n    emit([doc.coord.sort, doc.coord.x, doc.coord.y], doc);\n  }\n}"
}