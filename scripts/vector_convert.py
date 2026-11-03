#!/usr/bin/env python3
"""
Bidirectional converter between Android VectorDrawable (.xml) and Scalable Vector Graphics (.svg).

Usage:
    python3 scripts/vector_convert.py input.xml [output.svg]
    python3 scripts/vector_convert.py input.svg [output.xml]
"""

import sys
import os
import xml.etree.ElementTree as ET

ANDROID_NS = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('android', ANDROID_NS)


def android_color_to_svg(c: str):
    """Converts Android color (e.g. #AARRGGBB or @android:color/transparent) to standard SVG color."""
    if not c or c in ('@android:color/transparent', 'transparent', 'none'):
        return 'none', None
    c = c.strip()
    if c.startswith('#') and len(c) == 9:  # #AARRGGBB
        a_hex = c[1:3]
        rgb = '#' + c[3:]
        alpha = int(a_hex, 16) / 255.0
        if alpha >= 0.999:
            return rgb, None
        return rgb, f"{alpha:.2g}"
    return c, None


def svg_color_to_android(c: str, opacity=None):
    """Converts SVG color (e.g. #RRGGBB, #RRGGBBAA, black, none) to Android #AARRGGBB."""
    if not c or c in ('none', 'transparent'):
        return '@android:color/transparent'
    c = c.strip()
    if c.lower() == 'black':
        c = '#000000'
    elif c.lower() == 'white':
        c = '#ffffff'

    if c.startswith('#'):
        hex_str = c[1:]
        if len(hex_str) == 3:
            hex_str = ''.join([ch * 2 for ch in hex_str])
        if len(hex_str) == 6:
            alpha = int(float(opacity) * 255) if opacity is not None else 255
            return f"#{alpha:02X}{hex_str.upper()}"
        elif len(hex_str) == 8:  # SVG 2 #RRGGBBAA
            rr, gg, bb, aa = hex_str[0:2], hex_str[2:4], hex_str[4:6], hex_str[6:8]
            return f"#{aa}{rr}{gg}{bb}".upper()
    return c


def vd_to_svg(vd_path: str, svg_path: str):
    tree = ET.parse(vd_path)
    root = tree.getroot()

    vp_w = root.get(f'{{{ANDROID_NS}}}viewportWidth', '24')
    vp_h = root.get(f'{{{ANDROID_NS}}}viewportHeight', '24')

    # Default to 800px display canvas (with matching viewBox) for comfortable editing in Inkscape/Figma/Illustrator
    svg = ET.Element('svg', {
        'xmlns': 'http://www.w3.org/2000/svg',
        'width': '800px',
        'height': '800px',
        'viewBox': f'0 0 {vp_w} {vp_h}',
    })

    for elem in root.iter():
        tag = elem.tag.split('}')[-1]
        if tag == 'path':
            d = elem.get(f'{{{ANDROID_NS}}}pathData', '')
            fill_c = elem.get(f'{{{ANDROID_NS}}}fillColor', '')
            stroke_c = elem.get(f'{{{ANDROID_NS}}}strokeColor', '')
            stroke_w = elem.get(f'{{{ANDROID_NS}}}strokeWidth', '')
            fill_type = elem.get(f'{{{ANDROID_NS}}}fillType', '')

            fill_svg, fill_op = android_color_to_svg(fill_c)
            stroke_svg, stroke_op = android_color_to_svg(stroke_c)

            attribs = {'d': d}
            attribs['fill'] = fill_svg
            if fill_op:
                attribs['fill-opacity'] = fill_op

            if stroke_svg != 'none':
                attribs['stroke'] = stroke_svg
                if stroke_op:
                    attribs['stroke-opacity'] = stroke_op
                if stroke_w:
                    attribs['stroke-width'] = stroke_w

            if fill_type == 'evenOdd':
                attribs['fill-rule'] = 'evenodd'

            ET.SubElement(svg, 'path', attribs)

    ET.indent(svg, space='  ')
    with open(svg_path, 'wb') as f:
        f.write(ET.tostring(svg, encoding='utf-8', xml_declaration=True))
    print(f"Successfully converted: {vd_path} -> {svg_path}")


def svg_to_vd(svg_path: str, vd_path: str):
    tree = ET.parse(svg_path)
    root = tree.getroot()

    view_box = root.get('viewBox', '').split()
    if len(view_box) == 4:
        vp_w, vp_h = view_box[2], view_box[3]
    else:
        vp_w = root.get('width', '24').replace('px', '')
        vp_h = root.get('height', '24').replace('px', '')

    vd = ET.Element('vector', {
        f'{{{ANDROID_NS}}}width': '24dp',
        f'{{{ANDROID_NS}}}height': '24dp',
        f'{{{ANDROID_NS}}}viewportWidth': str(vp_w),
        f'{{{ANDROID_NS}}}viewportHeight': str(vp_h),
    })

    for elem in root.iter():
        tag = elem.tag.split('}')[-1]
        if tag == 'path':
            d = elem.get('d', '')
            if not d:
                continue
            fill = elem.get('fill', '#000000')
            fill_opacity = elem.get('fill-opacity', None)
            stroke = elem.get('stroke', '')
            stroke_opacity = elem.get('stroke-opacity', None)
            stroke_w = elem.get('stroke-width', '')
            fill_rule = elem.get('fill-rule', '')

            attribs = {
                f'{{{ANDROID_NS}}}pathData': d,
                f'{{{ANDROID_NS}}}fillColor': svg_color_to_android(fill, fill_opacity),
            }

            if stroke and stroke != 'none':
                attribs[f'{{{ANDROID_NS}}}strokeColor'] = svg_color_to_android(stroke, stroke_opacity)
                if stroke_w:
                    attribs[f'{{{ANDROID_NS}}}strokeWidth'] = stroke_w

            if fill_rule == 'evenodd':
                attribs[f'{{{ANDROID_NS}}}fillType'] = 'evenOdd'

            ET.SubElement(vd, 'path', attribs)
        elif tag == 'circle':
            cx = float(elem.get('cx', '0'))
            cy = float(elem.get('cy', '0'))
            r = float(elem.get('r', '0'))
            fill = elem.get('fill', '#000000')
            stroke = elem.get('stroke', '')
            stroke_w = elem.get('stroke-width', '')

            d = f"M{cx},{cy-r} A{r},{r} 0 1,1 {cx},{cy+r} A{r},{r} 0 1,1 {cx},{cy-r} Z"
            attribs = {
                f'{{{ANDROID_NS}}}pathData': d,
                f'{{{ANDROID_NS}}}fillColor': svg_color_to_android(fill),
            }
            if stroke and stroke != 'none':
                attribs[f'{{{ANDROID_NS}}}strokeColor'] = svg_color_to_android(stroke)
                if stroke_w:
                    attribs[f'{{{ANDROID_NS}}}strokeWidth'] = stroke_w

            ET.SubElement(vd, 'path', attribs)

    ET.indent(vd, space='    ')
    with open(vd_path, 'wb') as f:
        f.write(b'<?xml version="1.0" encoding="utf-8"?>\n')
        f.write(ET.tostring(vd, encoding='utf-8'))
    print(f"Successfully converted: {svg_path} -> {vd_path}")


def main():
    if len(sys.argv) < 2:
        print(__doc__.strip())
        sys.exit(1)

    in_file = sys.argv[1]
    if not os.path.exists(in_file):
        print(f"Error: file not found: {in_file}")
        sys.exit(1)

    base, ext = os.path.splitext(in_file)
    ext = ext.lower()

    if ext == '.xml':
        out_file = sys.argv[2] if len(sys.argv) > 2 else f"{base}.svg"
        vd_to_svg(in_file, out_file)
    elif ext == '.svg':
        out_file = sys.argv[2] if len(sys.argv) > 2 else f"{base}.xml"
        svg_to_vd(in_file, out_file)
    else:
        print(f"Unsupported extension: {ext}. Expected .xml or .svg")
        sys.exit(1)


if __name__ == '__main__':
    main()
