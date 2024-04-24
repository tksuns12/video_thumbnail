//
//  Params.swift
//  video_thumbnail
//
//  Created by 강현길 on 4/17/24.
//

import Foundation

struct Params {
    var video: String
    var headers: Dictionary<String, String>?
    var format: Int
    var maxh: Int
    var maxw: Int
    var timeMs: Int
    var quality: Int
}
