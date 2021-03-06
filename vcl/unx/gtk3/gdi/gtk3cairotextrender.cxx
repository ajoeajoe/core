/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

#include "gtk3cairotextrender.hxx"

GtkCairoTextRender::GtkCairoTextRender(GtkSalGraphics& rParent)
    : mrParent(rParent)
{
}

GlyphCache& GtkCairoTextRender::getPlatformGlyphCache()
{
    return mrParent.getPlatformGlyphCache();
}

cairo_t* GtkCairoTextRender::getCairoContext()
{
    return mrParent.getCairoContext();
}

void GtkCairoTextRender::getSurfaceOffset(double& nDX, double& nDY)
{
    nDX = 0;
    nDY = 0;
}

void GtkCairoTextRender::clipRegion(cairo_t* cr)
{
    mrParent.clipRegion(cr);
}

void GtkCairoTextRender::drawSurface(cairo_t* /*cr*/)
{
}

/* vim:set shiftwidth=4 softtabstop=4 expandtab: */
