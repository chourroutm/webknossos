# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: Annotation.proto
"""Generated protocol buffer code."""
from google.protobuf.internal import builder as _builder
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\x10\x41nnotation.proto\x12&com.scalableminds.webknossos.datastore\"\x88\x02\n\x0f\x41nnotationProto\x12\x13\n\x0b\x64\x65scription\x18\x01 \x02(\t\x12\x0f\n\x07version\x18\x02 \x02(\x03\x12V\n\x10\x61nnotationLayers\x18\x03 \x03(\x0b\x32<.com.scalableminds.webknossos.datastore.AnnotationLayerProto\x12!\n\x19\x65\x61rliestAccessibleVersion\x18\x04 \x02(\x03\x12%\n\x1dskeletonMayHavePendingUpdates\x18\x05 \x01(\x08\x12-\n%editableMappingsMayHavePendingUpdates\x18\x06 \x01(\x08\"\x86\x01\n\x14\x41nnotationLayerProto\x12\x11\n\ttracingId\x18\x01 \x02(\t\x12\x0c\n\x04name\x18\x02 \x02(\t\x12M\n\x03typ\x18\x03 \x02(\x0e\x32@.com.scalableminds.webknossos.datastore.AnnotationLayerTypeProto*4\n\x18\x41nnotationLayerTypeProto\x12\x0c\n\x08Skeleton\x10\x01\x12\n\n\x06Volume\x10\x02')

_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, globals())
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, 'Annotation_pb2', globals())
if _descriptor._USE_C_DESCRIPTORS == False:

  DESCRIPTOR._options = None
  _ANNOTATIONLAYERTYPEPROTO._serialized_start=464
  _ANNOTATIONLAYERTYPEPROTO._serialized_end=516
  _ANNOTATIONPROTO._serialized_start=61
  _ANNOTATIONPROTO._serialized_end=325
  _ANNOTATIONLAYERPROTO._serialized_start=328
  _ANNOTATIONLAYERPROTO._serialized_end=462
# @@protoc_insertion_point(module_scope)
