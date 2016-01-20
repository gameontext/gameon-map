{
  "map": "function(doc) {\n  if ( doc.coord ) {\n    emit([doc.coord.sort, doc.coord.x, doc.coord.y], doc);\n  }\n}"
}