import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { chromium } from 'playwright';

const urls = process.argv.slice(2);
assert.ok(urls.length, 'Pass at least one running browser build URL.');
const browser = await chromium.launch({
    executablePath: process.env.KOLAM_CHROME_PATH || undefined,
    args: ['--enable-webgl', '--use-angle=swiftshader', '--enable-unsafe-swiftshader'],
});
try {
    for (const url of urls) {
        const context = await browser.newContext({ viewport: { width: 1150, height: 750 } });
        try {
            const page = await context.newPage();
            const errors = [];
            page.on('pageerror', error => errors.push(error.message));
            const text = name => page.getByText(name, { exact: true });
            // The semantic DOM supplies bounds. The canvas receives the actual click.
            const click = name => text(name).click({ force: true });
            await page.goto(new URL('#lesson/join', url).href);
            await text('Join two loops').waitFor();
            await click('Change crossing');
            await text('Horizontal mirror joined loops (2 → 1).').waitFor();
            assert.equal(new URL(page.url()).hash, '#AgIB');
            await click('Save');
            await text('Saved as Kolam 1').waitFor();
            await page.reload();
            await click('Collection (1)');
            await click('Open');
            assert.equal(new URL(page.url()).hash, '#AgIB');
            const download = page.waitForEvent('download');
            await click('Export PNG');
            const png = await readFile(await (await download).path());
            assert.equal(png.subarray(0, 8).toString('hex'), '89504e470d0a1a0a');
            assert.equal(png.readUInt32BE(16), 1200);
            assert.equal(png.readUInt32BE(20), 1200);
            assert.deepEqual(errors, []);
            console.log(`PASS ${url} — fixed lesson, collection reload, and 1200 × 1200 PNG`);
        } finally {
            await context.close();
        }
    }
} finally {
    await browser.close();
}
